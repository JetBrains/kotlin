/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.common.collect.Lists
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*
import javax.swing.Icon

open class KtLightClassForExplicitDeclaration(
        protected val classFqName: FqName, // FqName of (possibly inner) class
        protected val classOrObject: KtClassOrObject)
: KtWrappingLightClass(classOrObject.manager), KtJavaMirrorMarker, StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {
    private val lightIdentifier = KtLightIdentifier(this, classOrObject)

    private fun getLocalClassParent(): PsiElement? {
        fun getParentByPsiMethod(method: PsiMethod?, name: String?, forceMethodWrapping: Boolean): PsiElement? {
            if (method == null || name == null) return null

            var containingClass: PsiClass? = method.containingClass ?: return null

            val currentFileName = classOrObject.containingFile.name

            var createWrapper = forceMethodWrapping
            // Use PsiClass wrapper instead of package light class to avoid names like "FooPackage" in Type Hierarchy and related views
            if (containingClass is KtLightClassForFacade) {
                containingClass = object : LightClass(containingClass as KtLightClassForFacade, KotlinLanguage.INSTANCE) {
                    override fun getName(): String? {
                        return currentFileName
                    }
                }
                createWrapper = true
            }

            if (createWrapper) {
                return object : LightMethod(myManager, method, containingClass!!, KotlinLanguage.INSTANCE) {
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

        var declaration: PsiElement? = KtPsiUtil.getTopmostParentOfTypes(
                classOrObject,
                KtNamedFunction::class.java,
                KtConstructor::class.java,
                KtProperty::class.java,
                KtAnonymousInitializer::class.java,
                KtParameter::class.java)

        if (declaration is KtParameter) {
            declaration = declaration.getStrictParentOfType<KtNamedDeclaration>()
        }

        if (declaration is KtFunction) {
            return getParentByPsiMethod(LightClassUtil.getLightClassMethod(declaration), declaration.name, false)
        }

        // Represent the property as a fake method with the same name
        if (declaration is KtProperty) {
            return getParentByPsiMethod(LightClassUtil.getLightClassPropertyMethods(declaration).getter, declaration.name, true)
        }

        if (declaration is KtAnonymousInitializer) {
            val parent = declaration.parent
            val grandparent = parent.parent

            if (parent is KtClassBody && grandparent is KtClassOrObject) {
                return grandparent.toLightClass()
            }
        }

        if (declaration is KtClass) {
            return declaration.toLightClass()
        }
        return null
    }

    private val _parent: PsiElement? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (classOrObject.isLocal())
            getLocalClassParent()
        else if (classOrObject.isTopLevel())
            containingFile
        else
            containingClass
    }

    override val kotlinOrigin: KtClassOrObject = classOrObject

    override fun getFqName(): FqName = classFqName

    override fun copy(): PsiElement {
        return KtLightClassForExplicitDeclaration(classFqName, classOrObject.copy() as KtClassOrObject)
    }

    override val clsDelegate: PsiClass by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val javaFileStub = getJavaFileStub()

        LightClassUtil.findClass(classFqName, javaFileStub) ?: run {
            val outermostClassOrObject = getOutermostClassOrObject(classOrObject)
            val ktFileText: String? = try {
                outermostClassOrObject.containingFile.text
            }
            catch (e: Exception) {
                "Can't get text for outermost class"
            }

            val stubFileText = DebugUtil.stubTreeToString(javaFileStub)
            throw IllegalStateException("Class was not found $classFqName\nin $ktFileText\nstub: \n$stubFileText")
        }
    }

    private fun getJavaFileStub(): PsiJavaFileStub = getLightClassData().javaFileStub

    protected fun getDescriptor(): ClassDescriptor? {
        return LightClassGenerationSupport.getInstance(project).resolveToDescriptor(classOrObject) as? ClassDescriptor
    }

    private fun getLightClassData(): OutermostKotlinClassLightClassData {
        val lightClassData = getLightClassData(classOrObject)
        if (lightClassData !is OutermostKotlinClassLightClassData) {
            LOG.error("Invalid light class data for existing light class:\n$lightClassData\n${classOrObject.getElementTextWithContext()}")
        }
        return lightClassData as OutermostKotlinClassLightClassData
    }

    private val _containingFile: PsiFile by lazy(LazyThreadSafetyMode.PUBLICATION) {
        classOrObject.containingFile.virtualFile ?: error("No virtual file for " + classOrObject.text)

        object : FakeFileForLightClass(
                classOrObject.getContainingKtFile(),
                { if (classOrObject.isTopLevel()) this else create(getOutermostClassOrObject(classOrObject))!! },
                { getJavaFileStub() }
        ) {
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
        return another is PsiClass && Comparing.equal(another.qualifiedName, qualifiedName)
    }

    override fun getElementIcon(flags: Int): Icon? {
        throw UnsupportedOperationException("This should be done by JetIconProvider")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val aClass = other as KtLightClassForExplicitDeclaration

        if (classFqName != aClass.classFqName) return false

        return true
    }

    override fun hashCode(): Int = classFqName.hashCode()

    override fun getContainingClass(): PsiClass? {
        if (classOrObject.parent === classOrObject.containingFile) return null

        val containingClassOrObject = (classOrObject.parent as? KtClassBody)?.parent as? KtClassOrObject
        if (containingClassOrObject != null) {
            return create(containingClassOrObject)
        }

        // TODO: should return null
        return super.getContainingClass()
    }

    override fun getParent(): PsiElement? = _parent

    private val _typeParameterList: PsiTypeParameterList by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LightClassUtil.buildLightTypeParameterList(this, classOrObject)
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList.typeParameters

    override fun getName(): String? = classFqName.shortName().asString()

    override fun getQualifiedName(): String = classFqName.asString()

    private val _modifierList : PsiModifierList by lazy(LazyThreadSafetyMode.PUBLICATION) {
        object : KtLightModifierListWithExplicitModifiers(this@KtLightClassForExplicitDeclaration, computeModifiers()) {
            override val delegate: PsiAnnotationOwner
                get() = this@KtLightClassForExplicitDeclaration.delegate.modifierList!!
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
            // Top-level private class has PACKAGE_LOCAL visibility in Java
            // Nested private class has PRIVATE visibility
            psiModifiers.add(if (classOrObject.isTopLevel()) PsiModifier.PACKAGE_LOCAL else PsiModifier.PRIVATE)
        }
        else if (!psiModifiers.contains(PsiModifier.PROTECTED)) {
            psiModifiers.add(PsiModifier.PUBLIC)
        }


        // FINAL
        if (isAbstract() || isSealed()) {
            psiModifiers.add(PsiModifier.ABSTRACT)
        }
        else if (!(classOrObject.hasModifier(OPEN_KEYWORD) || (classOrObject is KtClass && classOrObject.isEnum()))) {
            psiModifiers.add(PsiModifier.FINAL)
        }

        if (!classOrObject.isTopLevel() && !classOrObject.hasModifier(INNER_KEYWORD)) {
            psiModifiers.add(PsiModifier.STATIC)
        }

        return psiModifiers.toTypedArray()
    }

    private fun isAbstract(): Boolean = classOrObject.hasModifier(ABSTRACT_KEYWORD) || isInterface

    private fun isSealed(): Boolean = classOrObject.hasModifier(SEALED_KEYWORD)

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList.hasModifierProperty(name)

    override fun isDeprecated(): Boolean {
        val jetModifierList = classOrObject.modifierList ?: return false

        val deprecatedFqName = KotlinBuiltIns.FQ_NAMES.deprecated
        val deprecatedName = deprecatedFqName.shortName().asString()

        for (annotationEntry in jetModifierList.annotationEntries) {
            val typeReference = annotationEntry.typeReference ?: continue

            val typeElement = typeReference.typeElement
            if (typeElement !is KtUserType) continue // If it's not a user type, it's definitely not a ref to deprecated

            val fqName = toQualifiedName(typeElement) ?: continue

            if (deprecatedFqName == fqName) return true
            if (deprecatedName == fqName.asString()) return true
        }
        return false
    }

    private fun toQualifiedName(userType: KtUserType): FqName? {
        val reversedNames = Lists.newArrayList<String>()

        var current: KtUserType? = userType
        while (current != null) {
            val name = current.referencedName ?: return null

            reversedNames.add(name)
            current = current.qualifier
        }

        return FqName.fromSegments(ContainerUtil.reverse(reversedNames))
    }

    override fun isInterface(): Boolean {
        if (classOrObject !is KtClass) return false
        return classOrObject.isInterface() || classOrObject.isAnnotation()
    }

    override fun isAnnotationType(): Boolean = classOrObject is KtClass && classOrObject.isAnnotation()

    override fun isEnum(): Boolean = classOrObject is KtClass && classOrObject.isEnum()

    override fun hasTypeParameters(): Boolean = classOrObject is KtClass && !classOrObject.typeParameters.isEmpty()

    override fun isValid(): Boolean = classOrObject.isValid

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        val qualifiedName: String?
        if (baseClass is KtLightClassForExplicitDeclaration) {
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
        kotlinOrigin.setName(name)
        return this
    }

    override fun toString() = "${KtLightClass::class.java.simpleName}:$classFqName"

    override fun getOwnInnerClasses(): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        classOrObject.declarations.filterIsInstance<KtClassOrObject>().mapNotNullTo(result) { create(it) }

        if (classOrObject.hasInterfaceDefaultImpls) {
            result.add(KtLightClassForInterfaceDefaultImpls(classFqName.defaultImplsChild(), classOrObject))
        }
        return result
    }

    override fun getUseScope(): SearchScope = kotlinOrigin.useScope

    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObject.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = classOrObject.stub

    override fun getNameIdentifier() = lightIdentifier

    companion object {
        private val JAVA_API_STUB = Key.create<CachedValue<WithFileStubAndExtraDiagnostics>>("JAVA_API_STUB")

        private val jetTokenToPsiModifier = listOf(
                PUBLIC_KEYWORD to PsiModifier.PUBLIC,
                INTERNAL_KEYWORD to  PsiModifier.PUBLIC,
                PROTECTED_KEYWORD to PsiModifier.PROTECTED,
                FINAL_KEYWORD to PsiModifier.FINAL)


        fun create(classOrObject: KtClassOrObject): KtLightClassForExplicitDeclaration? {
            val fqName = predictFqName(classOrObject) ?: return null

            if (classOrObject is KtObjectDeclaration && classOrObject.isObjectLiteral()) {
                return KtLightClassForAnonymousDeclaration(fqName, classOrObject)
            }

            return KtLightClassForExplicitDeclaration(fqName, classOrObject)
        }

        private fun predictFqName(classOrObject: KtClassOrObject): FqName? {
            if (classOrObject.isLocal()) {
                if (classOrObject.containingFile.virtualFile == null) return null
                val data = getLightClassDataExactly(classOrObject)
                return data?.jvmQualifiedName
            }
            val internalName = PsiCodegenPredictor.getPredefinedJvmInternalName(classOrObject, NoResolveFileClassesProvider)
            return if (internalName == null) null else JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars
        }

        fun getLightClassData(classOrObject: KtClassOrObject): LightClassData {
            return getLightClassCachedValue(classOrObject).value
        }

        fun getLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<WithFileStubAndExtraDiagnostics> {
            val outermostClassOrObject = getOutermostClassOrObject(classOrObject)
            var value = outermostClassOrObject.getUserData(JAVA_API_STUB)
            if (value == null) {
                value = CachedValuesManager.getManager(classOrObject.project).createCachedValue(
                        LightClassDataProviderForClassOrObject(outermostClassOrObject), false)
                value = outermostClassOrObject.putUserDataIfAbsent(JAVA_API_STUB, value)
            }
            return value
        }

        private fun getLightClassDataExactly(classOrObject: KtClassOrObject): LightClassDataForKotlinClass? {
            val data = getLightClassData(classOrObject) as? OutermostKotlinClassLightClassData ?: return null
            return data.dataForClass(classOrObject)
        }

        private fun getOutermostClassOrObject(classOrObject: KtClassOrObject): KtClassOrObject {
            val outermostClass = KtPsiUtil.getOutermostClassOrObject(classOrObject) ?:
                throw IllegalStateException("Attempt to build a light class for a local class: " + classOrObject.text)

            return outermostClass
        }

        private fun checkSuperTypeByFQName(classDescriptor: ClassDescriptor, qualifiedName: String, deep: Boolean): Boolean {
            if (CommonClassNames.JAVA_LANG_OBJECT == qualifiedName) return true

            if (qualifiedName == DescriptorUtils.getFqName(classDescriptor).asString()) return true

            val fqName = FqNameUnsafe(qualifiedName)
            val mappedDescriptor =
                    if (fqName.isSafe)
                        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(fqName.toSafe(), classDescriptor.builtIns)
                    else null
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

        private val LOG = Logger.getInstance(KtLightClassForExplicitDeclaration::class.java)
    }
}
