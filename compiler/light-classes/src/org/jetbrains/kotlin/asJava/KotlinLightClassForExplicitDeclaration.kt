/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.PsiClassHolderFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.lexer.JetTokens.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import javax.swing.Icon

public open class KotlinLightClassForExplicitDeclaration(
        manager: PsiManager,
        protected val classFqName: FqName, // FqName of (possibly inner) class
        protected val classOrObject: JetClassOrObject)
: KotlinWrappingLightClass(manager), JetJavaMirrorMarker, StubBasedPsiElement<KotlinClassOrObjectStub<out JetClassOrObject>> {
    private var delegate: PsiClass? = null

    private fun getLocalClassParent(): PsiElement? {
        fun getParentByPsiMethod(method: PsiMethod?, name: String?, forceMethodWrapping: Boolean): PsiElement? {
            if (method == null || name == null) return null

            var containingClass: PsiClass? = method.containingClass ?: return null

            val currentFileName = classOrObject.containingFile.name

            var createWrapper = forceMethodWrapping
            // Use PsiClass wrapper instead of package light class to avoid names like "FooPackage" in Type Hierarchy and related views
            if (containingClass is KotlinLightClassForFacade) {
                containingClass = object : LightClass(containingClass as KotlinLightClassForFacade, JetLanguage.INSTANCE) {
                    override fun getName(): String? {
                        return currentFileName
                    }
                }
                createWrapper = true
            }

            if (createWrapper) {
                return object : LightMethod(myManager, method, containingClass!!, JetLanguage.INSTANCE) {
                    override fun getParent(): PsiElement {
                        return getContainingClass()!!
                    }

                    override fun getName(): String {
                        return name
                    }
                }
            }

            return method
        }

        var declaration: PsiElement? = JetPsiUtil.getTopmostParentOfTypes(
                classOrObject,
                JetNamedFunction::class.java,
                JetConstructor::class.java,
                JetProperty::class.java,
                JetClassInitializer::class.java,
                JetParameter::class.java)

        if (declaration is JetParameter) {
            declaration = declaration.getStrictParentOfType<JetNamedDeclaration>()
        }

        if (declaration is JetFunction) {
            return getParentByPsiMethod(LightClassUtil.getLightClassMethod(declaration), declaration.name, false)
        }

        // Represent the property as a fake method with the same name
        if (declaration is JetProperty) {
            return getParentByPsiMethod(LightClassUtil.getLightClassPropertyMethods(declaration).getter, declaration.name, true)
        }

        if (declaration is JetClassInitializer) {
            val parent = declaration.parent
            val grandparent = parent.parent

            if (parent is JetClassBody && grandparent is JetClassOrObject) {
                return LightClassUtil.getPsiClass(grandparent)
            }
        }

        if (declaration is JetClass) {
            return LightClassUtil.getPsiClass(declaration)
        }
        return null
    }

    private val _parent: PsiElement? by lazy {
        if (classOrObject.isLocal())
            getLocalClassParent()
        else if (classOrObject.parent === classOrObject.containingFile)
            containingFile
        else
            containingClass
    }

    override fun getOrigin(): JetClassOrObject = classOrObject

    override fun getFqName(): FqName = classFqName

    override fun copy(): PsiElement {
        return KotlinLightClassForExplicitDeclaration(manager, classFqName, classOrObject.copy() as JetClassOrObject)
    }

    override fun getDelegate(): PsiClass {
        if (delegate == null) {
            val javaFileStub = getJavaFileStub()

            val psiClass = LightClassUtil.findClass(classFqName, javaFileStub)
            if (psiClass == null) {
                val outermostClassOrObject = getOutermostClassOrObject(classOrObject)
                val ktFileText: String? = try {
                    outermostClassOrObject.containingFile.text
                }
                catch (e: Exception) {
                    "Can't get text for outermost class"
                }

                val stubFileText: String? = try {
                    javaFileStub.psi.text
                }
                catch (e: Exception) {
                    "Can't get stub text"
                }

                throw IllegalStateException("Class was not found $classFqName\nin $ktFileText\nstub: \n$stubFileText")
            }
            delegate = psiClass
        }

        return delegate!!
    }

    private fun getJavaFileStub(): PsiJavaFileStub = getLightClassData().javaFileStub

    protected fun getDescriptor(): ClassDescriptor? {
        return LightClassGenerationSupport.getInstance(project).resolveClassToDescriptor(classOrObject)
    }

    private fun getLightClassData(): OutermostKotlinClassLightClassData {
        return getLightClassData(classOrObject)
    }

    private val _containingFile: PsiFile by lazy {
        val virtualFile = classOrObject.containingFile.virtualFile
        assert(virtualFile != null) { "No virtual file for " + classOrObject.text }

        object : ClsFileImpl(ClassFileViewProvider(getManager(), virtualFile)) {
            override fun getPackageName(): String {
                return classOrObject.getContainingJetFile().packageFqName.asString()
            }

            override fun getStub(): PsiClassHolderFileStub<PsiJavaFile> = getJavaFileStub()

            override fun processDeclarations(
                    processor: PsiScopeProcessor,
                    state: ResolveState,
                    lastParent: PsiElement?,
                    place: PsiElement): Boolean {
                if (!super.processDeclarations(processor, state, lastParent, place)) return false

                // We have to explicitly process package declarations if current file belongs to default package
                // so that Java resolve can find classes located in that package
                val packageName = packageName
                if (!packageName.isEmpty()) return true

                val aPackage = JavaPsiFacade.getInstance(myManager.project).findPackage(packageName)
                if (aPackage != null && !aPackage.processDeclarations(processor, state, null, place)) return false

                return true
            }
        }
    }

    override fun getContainingFile(): PsiFile? = _containingFile

    override fun getNavigationElement(): PsiElement = classOrObject

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return another is PsiClass && Comparing.equal(another.getQualifiedName(), qualifiedName)
    }

    override fun getElementIcon(flags: Int): Icon? {
        throw UnsupportedOperationException("This should be done byt JetIconProvider")
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val aClass = o as KotlinLightClassForExplicitDeclaration

        if (classFqName != aClass.classFqName) return false

        return true
    }

    override fun hashCode(): Int = classFqName.hashCode()

    override fun getContainingClass(): PsiClass? {
        if (classOrObject.parent === classOrObject.containingFile) return null
        return super.getContainingClass()
    }

    override fun getParent(): PsiElement? = _parent

    override fun getContext(): PsiElement? = getParent()

    private val _typeParameterList: PsiTypeParameterList by lazy {
        LightClassUtil.buildLightTypeParameterList(this, classOrObject)
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList.typeParameters

    override fun getName(): String = classFqName.shortName().asString()

    override fun getQualifiedName(): String = classFqName.asString()

    private val _modifierList : PsiModifierList by lazy {
        object : KotlinLightModifierList(this.manager, computeModifiers()) {
            override fun getDelegate(): PsiModifierList {
                return this@KotlinLightClassForExplicitDeclaration.getDelegate().modifierList!!
            }
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    protected open fun computeModifiers(): Array<String> {
        val psiModifiers = hashSetOf<String>()

        // PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL
        //noinspection unchecked

        for (tokenAndModifier in jetTokenToPsiModifier) {
            if (classOrObject.hasModifier(tokenAndModifier.first)) {
                psiModifiers.add(tokenAndModifier.second)
            }
        }

        if (classOrObject.hasModifier(PRIVATE_KEYWORD)) {
            // Top-level private class has PUBLIC visibility in Java
            // Nested private class has PRIVATE visibility
            psiModifiers.add(if (classOrObject.isTopLevel()) PsiModifier.PUBLIC else PsiModifier.PRIVATE)
        }

        if (!psiModifiers.contains(PsiModifier.PRIVATE) && !psiModifiers.contains(PsiModifier.PROTECTED)) {
            psiModifiers.add(PsiModifier.PUBLIC) // For internal (default) visibility
        }


        // FINAL
        if (isAbstract() || isSealed()) {
            psiModifiers.add(PsiModifier.ABSTRACT)
        }
        else if (!(classOrObject.hasModifier(OPEN_KEYWORD) || (classOrObject is JetClass && classOrObject.isEnum()))) {
            psiModifiers.add(PsiModifier.FINAL)
        }

        if (!classOrObject.isTopLevel() && !classOrObject.hasModifier(INNER_KEYWORD)) {
            psiModifiers.add(PsiModifier.STATIC)
        }

        return psiModifiers.toTypedArray()
    }

    private fun isAbstract(): Boolean = classOrObject.hasModifier(ABSTRACT_KEYWORD) || isInterface

    private fun isSealed(): Boolean = classOrObject.hasModifier(SEALED_KEYWORD)

    override fun hasModifierProperty(@NonNls name: String): Boolean = getModifierList().hasModifierProperty(name)

    override fun isDeprecated(): Boolean {
        val jetModifierList = classOrObject.modifierList ?: return false

        val deprecatedFqName = KotlinBuiltIns.FQ_NAMES.deprecated
        val deprecatedName = deprecatedFqName.shortName().asString()

        for (annotationEntry in jetModifierList.annotationEntries) {
            val typeReference = annotationEntry.typeReference ?: continue

            val typeElement = typeReference.typeElement
            if (typeElement !is JetUserType) continue // If it's not a user type, it's definitely not a ref to deprecated

            val fqName = JetPsiUtil.toQualifiedName(typeElement) ?: continue

            if (deprecatedFqName == fqName) return true
            if (deprecatedName == fqName.asString()) return true
        }
        return false
    }

    override fun isInterface(): Boolean {
        if (classOrObject !is JetClass) return false
        return classOrObject.isInterface() || classOrObject.isAnnotation()
    }

    override fun isAnnotationType(): Boolean = classOrObject is JetClass && classOrObject.isAnnotation()

    override fun isEnum(): Boolean = classOrObject is JetClass && classOrObject.isEnum()

    override fun hasTypeParameters(): Boolean = classOrObject is JetClass && !classOrObject.typeParameters.isEmpty()

    override fun isValid(): Boolean = classOrObject.isValid

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        val qualifiedName: String?
        if (baseClass is KotlinLightClassForExplicitDeclaration) {
            val baseDescriptor = baseClass.getDescriptor()
            qualifiedName = if (baseDescriptor != null) DescriptorUtils.getFqName(baseDescriptor).asString() else null
        }
        else {
            qualifiedName = baseClass.qualifiedName
        }

        val thisDescriptor = getDescriptor()
        return qualifiedName != null && thisDescriptor != null && checkSuperTypeByFQName(thisDescriptor, qualifiedName, checkDeep)
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String): PsiElement {
        getOrigin().setName(name)
        return this
    }

    override fun toString(): String {
        try {
            return javaClass<KotlinLightClass>().simpleName + ":" + qualifiedName
        }
        catch (e: Throwable) {
            return javaClass<KotlinLightClass>().simpleName + ":" + e.toString()
        }

    }

    override fun getOwnInnerClasses(): List<PsiClass> {
        return getDelegate().innerClasses
            .map {
                val declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(it) as JetClassOrObject?
                if (declaration != null) create(myManager, declaration, it) else null
            }
            .filterNotNull()
    }

    override fun getUseScope(): SearchScope = getOrigin().useScope

    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObject.elementType
    override fun getStub(): KotlinClassOrObjectStub<out JetClassOrObject>? = classOrObject.stub

    companion object {
        private val JAVA_API_STUB = Key.create<CachedValue<OutermostKotlinClassLightClassData>>("JAVA_API_STUB")

        private val jetTokenToPsiModifier = listOf(
                PUBLIC_KEYWORD to PsiModifier.PUBLIC,
                INTERNAL_KEYWORD to  PsiModifier.PUBLIC,
                PROTECTED_KEYWORD to PsiModifier.PROTECTED,
                FINAL_KEYWORD to PsiModifier.FINAL)


        @JvmOverloads
        public fun create(
                manager: PsiManager,
                classOrObject: JetClassOrObject,
                psiClass: PsiClass? = null
        ): KotlinLightClassForExplicitDeclaration? {
            if (LightClassUtil.belongsToKotlinBuiltIns(classOrObject.getContainingJetFile())) {
                return null
            }

            val fqName = predictFqName(classOrObject) ?: return null

            if (classOrObject is JetObjectDeclaration && classOrObject.isObjectLiteral()) {
                return KotlinLightClassForAnonymousDeclaration(manager, fqName, classOrObject)
            }

            if (classOrObject.hasInterfaceDefaultImpls) {
                val implsFqName = fqName.defaultImplsChild()
                if (implsFqName.asString() == psiClass?.qualifiedName) {
                    return KotlinLightClassForInterfaceDefaultImpls(manager, implsFqName, classOrObject)
                }
            }

            return KotlinLightClassForExplicitDeclaration(manager, fqName, classOrObject)
        }

        private fun predictFqName(classOrObject: JetClassOrObject): FqName? {
            if (classOrObject.isLocal()) {
                val data = getLightClassDataExactly(classOrObject)
                return data?.jvmQualifiedName
            }
            val internalName = PsiCodegenPredictor.getPredefinedJvmInternalName(classOrObject, NoResolveFileClassesProvider)
            return if (internalName == null) null else JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars
        }

        public fun getLightClassData(classOrObject: JetClassOrObject): OutermostKotlinClassLightClassData {
            return getLightClassCachedValue(classOrObject).value
        }

        public fun getLightClassCachedValue(classOrObject: JetClassOrObject): CachedValue<OutermostKotlinClassLightClassData> {
            val outermostClassOrObject = getOutermostClassOrObject(classOrObject)
            var value = outermostClassOrObject.getUserData(JAVA_API_STUB)
            if (value == null) {
                value = CachedValuesManager.getManager(classOrObject.project).createCachedValue(
                        KotlinJavaFileStubProvider.createForDeclaredClass(outermostClassOrObject), false)
                value = outermostClassOrObject.putUserDataIfAbsent(JAVA_API_STUB, value)!!
            }
            return value
        }

        private fun getLightClassDataExactly(classOrObject: JetClassOrObject): LightClassDataForKotlinClass? {
            val data = getLightClassData(classOrObject)
            return data.dataForClass(classOrObject)
        }

        private fun getOutermostClassOrObject(classOrObject: JetClassOrObject): JetClassOrObject {
            val outermostClass = JetPsiUtil.getOutermostClassOrObject(classOrObject) ?:
                throw IllegalStateException("Attempt to build a light class for a local class: " + classOrObject.text)

            return outermostClass
        }

        private fun checkSuperTypeByFQName(classDescriptor: ClassDescriptor, qualifiedName: String, deep: Boolean): Boolean {
            if (CommonClassNames.JAVA_LANG_OBJECT == qualifiedName) return true

            if (qualifiedName == DescriptorUtils.getFqName(classDescriptor).asString()) return true

            val fqName = FqNameUnsafe(qualifiedName)
            val mappedDescriptor = if (fqName.isSafe()) JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(fqName.toSafe()) else null
            val mappedQName = if (mappedDescriptor == null) null else DescriptorUtils.getFqName(mappedDescriptor).asString()
            if (qualifiedName == mappedQName) return true

            for (superType in classDescriptor.typeConstructor.supertypes) {
                val superDescriptor = superType.constructor.declarationDescriptor

                if (superDescriptor is ClassDescriptor) {
                    val superQName = DescriptorUtils.getFqName(superDescriptor).asString()
                    if (superQName == qualifiedName || superQName == mappedQName) return true

                    if (deep) {
                        if (checkSuperTypeByFQName(superDescriptor, qualifiedName, true)) {
                            return true
                        }
                    }
                }
            }

            return false
        }
    }
}
