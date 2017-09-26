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

package org.jetbrains.kotlin.asJava.classes

import com.google.common.collect.Lists
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.PsiSubstitutorImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult.UNSURE
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataProviderForClassOrObject
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.asJava.elements.KtLightPsiReferenceList
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*
import javax.swing.Icon

abstract class KtLightClassForSourceDeclaration(protected val classOrObject: KtClassOrObject)
    : KtLazyLightClass(classOrObject.manager), StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {
    private val lightIdentifier = KtLightIdentifier(this, classOrObject)

    private val _extendsList by lazyPub {
        val listDelegate = super.getExtendsList() ?: return@lazyPub null
        KtLightPsiReferenceList(listDelegate, this)
    }

    private val _implementsList by lazyPub {
        val listDelegate = super.getImplementsList() ?: return@lazyPub null
        KtLightPsiReferenceList(listDelegate, this)
    }

    override val kotlinOrigin: KtClassOrObject = classOrObject

    abstract override fun copy(): PsiElement
    abstract override fun getParent(): PsiElement?
    abstract override fun getQualifiedName(): String?

    override val lightClassData: LightClassData
        get() = findLightClassData()

    open protected fun findLightClassData() = getLightClassDataHolder().findDataForClassOrObject(classOrObject)

    private fun getJavaFileStub(): PsiJavaFileStub = getLightClassDataHolder().javaFileStub

    fun getDescriptor(): ClassDescriptor? {
        return LightClassGenerationSupport.getInstance(project).resolveToDescriptor(classOrObject) as? ClassDescriptor
    }

    protected fun getLightClassDataHolder(): LightClassDataHolder.ForClass {
        val lightClassData = getLightClassDataHolder(classOrObject)
        if (lightClassData is InvalidLightClassDataHolder) {
            LOG.error("Invalid light class data for existing light class:\n$lightClassData\n${classOrObject.getElementTextWithContext()}")
        }
        return lightClassData
    }

    private val _containingFile: PsiFile by lazyPub {
        object : FakeFileForLightClass(
                classOrObject.containingKtFile,
                { if (classOrObject.isTopLevel()) this else create(getOutermostClassOrObject(classOrObject))!! },
                { getJavaFileStub() }
        ) {
            override fun findReferenceAt(offset: Int) = ktFile.findReferenceAt(offset)

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
        return another is KtLightClassForSourceDeclaration && Comparing.equal(another.qualifiedName, qualifiedName)
    }

    override fun getElementIcon(flags: Int): Icon? {
        throw UnsupportedOperationException("This should be done by JetIconProvider")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.java != other::class.java) return false

        val aClass = other as KtLightClassForSourceDeclaration

        if (classOrObject != aClass.classOrObject) return false

        return true
    }

    override fun hashCode(): Int = classOrObject.hashCode()

    override fun getContainingClass(): PsiClass? {
        if (classOrObject.parent === classOrObject.containingFile) return null

        val containingClassOrObject = (classOrObject.parent as? KtClassBody)?.parent as? KtClassOrObject
        if (containingClassOrObject != null) {
            return create(containingClassOrObject)
        }

        // TODO: should return null
        return super.getContainingClass()
    }

    private val _typeParameterList: PsiTypeParameterList by lazyPub {
        LightClassUtil.buildLightTypeParameterList(this, classOrObject)
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList.typeParameters

    override fun getName(): String? = classOrObject.nameAsName?.asString()

    private val _modifierList: PsiModifierList by lazyPub { KtLightClassModifierList(this) }

    override fun getModifierList(): PsiModifierList? = _modifierList

    protected open fun computeModifiers(): Set<String> {
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

        return psiModifiers
    }

    private fun isAbstract(): Boolean = classOrObject.hasModifier(ABSTRACT_KEYWORD) || isInterface

    private fun isSealed(): Boolean = classOrObject.hasModifier(SEALED_KEYWORD)

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false

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
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val qualifiedName: String? = if (baseClass is KtLightClassForSourceDeclaration) {
            val baseDescriptor = baseClass.getDescriptor()
            if (baseDescriptor != null) DescriptorUtils.getFqName(baseDescriptor).asString() else null
        }
        else {
            baseClass.qualifiedName
        }

        val thisDescriptor = getDescriptor()
        return qualifiedName != null && thisDescriptor != null && checkSuperTypeByFQName(thisDescriptor, qualifiedName, checkDeep)
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String): PsiElement {
        kotlinOrigin.setName(name)
        return this
    }

    override fun toString() = "${this::class.java.simpleName}:${classOrObject.getDebugText()}"

    override fun getOwnInnerClasses(): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        classOrObject.declarations.filterIsInstance<KtClassOrObject>()
                // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
                // inner classes with null names can't be searched for and can't be used from java anyway
                // we can't prohibit creating light classes with null names either since they can contain members
                .filter { it.name != null }
                .mapNotNullTo(result) { create(it) }

        if (classOrObject.hasInterfaceDefaultImpls) {
            result.add(KtLightClassForInterfaceDefaultImpls(classOrObject))
        }
        return result
    }

    override fun getUseScope(): SearchScope = kotlinOrigin.useScope

    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObject.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = classOrObject.stub

    override fun getNameIdentifier(): KtLightIdentifier? = lightIdentifier

    override fun getExtendsList() = _extendsList

    override fun getImplementsList() = _implementsList

    companion object {
        private val JAVA_API_STUB = Key.create<CachedValue<LightClassDataHolder.ForClass>>("JAVA_API_STUB")

        private val jetTokenToPsiModifier = listOf(
                PUBLIC_KEYWORD to PsiModifier.PUBLIC,
                INTERNAL_KEYWORD to PsiModifier.PUBLIC,
                PROTECTED_KEYWORD to PsiModifier.PROTECTED,
                FINAL_KEYWORD to PsiModifier.FINAL)


        fun create(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? {
            if (classOrObject.hasExpectModifier()) {
                return null
            }

            if (classOrObject is KtObjectDeclaration && classOrObject.isObjectLiteral()) {
                if (classOrObject.containingFile.virtualFile == null) return null

                return KtLightClassForAnonymousDeclaration(classOrObject)
            }

            if (isEnumEntryWithoutBody(classOrObject)) {
                return null
            }

            if (classOrObject.isLocal) {
                if (classOrObject.containingFile.virtualFile == null) return null

                return KtLightClassForLocalDeclaration(classOrObject)
            }


            return KtLightClassImpl(classOrObject)
        }

        private fun isEnumEntryWithoutBody(classOrObject: KtClassOrObject): Boolean {
            if (classOrObject !is KtEnumEntry) {
                return false
            }
            return classOrObject.getBody()?.declarations?.isEmpty() ?: true
        }

        fun getLightClassDataHolder(classOrObject: KtClassOrObject): LightClassDataHolder.ForClass {
            return classOrObject.containingKtFile.script?.let { KtLightClassForScript.getLightClassCachedValue(it).value } ?:
                   getLightClassCachedValue(classOrObject).value
        }

        private fun getLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<LightClassDataHolder.ForClass> {
            var value =
                    getOutermostClassOrObject(classOrObject).getUserData(JAVA_API_STUB) // stub computed for outer class can be used for inner/nested
                    ?: classOrObject.getUserData(JAVA_API_STUB)
            if (value == null) {
                value = CachedValuesManager.getManager(classOrObject.project).createCachedValue(
                        LightClassDataProviderForClassOrObject(classOrObject), false)
                value = classOrObject.putUserDataIfAbsent(JAVA_API_STUB, value)
            }
            return value
        }

        private fun checkSuperTypeByFQName(classDescriptor: ClassDescriptor, qualifiedName: String, deep: Boolean): Boolean {
            if (CommonClassNames.JAVA_LANG_OBJECT == qualifiedName) return true

            if (qualifiedName == DescriptorUtils.getFqName(classDescriptor).asString()) return true

            val fqName = FqNameUnsafe(qualifiedName)
            val mappedQName =
                    if (fqName.isSafe)
                        JavaToKotlinClassMap.mapJavaToKotlin(fqName.toSafe())?.asSingleFqName()?.asString()
                    else null
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

        private val LOG = Logger.getInstance(KtLightClassForSourceDeclaration::class.java)
    }

    override fun getSupers(): Array<PsiClass> {
        if (classOrObject.superTypeListEntries.isEmpty()) {
            return getSupertypeByPsi()?.let { arrayOf(it.resolve()!!) } ?: emptyArray()
        }
        return clsDelegate.supers
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        if (classOrObject.superTypeListEntries.isEmpty()) {
            return getSupertypeByPsi()?.let { arrayOf<PsiClassType>(it) } ?: emptyArray()
        }
        return clsDelegate.superTypes
    }

    private fun getSupertypeByPsi(): PsiImmediateClassType? {
        return classOrObject.defaultJavaAncestorQualifiedName()?.let {
            ancestorFqName ->
            JavaPsiFacade.getInstance(project).findClass(ancestorFqName, resolveScope)?.let {
                PsiImmediateClassType(it, createSubstitutor(it))
            }
        }
    }

    private fun createSubstitutor(ancestor: PsiClass): PsiSubstitutor {
        if (ancestor.qualifiedName != CommonClassNames.JAVA_LANG_ENUM) {
            return PsiSubstitutor.EMPTY
        }
        val javaLangEnumsTypeParameter = ancestor.typeParameters.firstOrNull() ?: return PsiSubstitutor.EMPTY
        return PsiSubstitutorImpl.createSubstitutor(
                mapOf(
                        javaLangEnumsTypeParameter to PsiImmediateClassType(this, PsiSubstitutor.EMPTY)
                )
        )
    }

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    private class KtLightClassModifierList(containingClass: KtLightClassForSourceDeclaration)
        : KtLightModifierList<KtLightClassForSourceDeclaration>(containingClass) {

        private val modifiers by lazyPub { containingClass.computeModifiers() }

        override fun hasModifierProperty(name: String): Boolean {
            if (name != PsiModifier.FINAL) {
                return name in modifiers
            }

            val isFinalByPsi = PsiModifier.FINAL in modifiers
            // annotations can make class open via 'allopen' plugin
            if (!owner.isPossiblyAffectedByAllOpen() || !isFinalByPsi) return isFinalByPsi

            return clsDelegate.hasModifierProperty(PsiModifier.FINAL)
        }
    }
}

fun KtLightClassForSourceDeclaration.isPossiblyAffectedByAllOpen() = !isAnnotationType && !isInterface && kotlinOrigin.annotationEntries.isNotEmpty()

fun getOutermostClassOrObject(classOrObject: KtClassOrObject): KtClassOrObject {
    val outermostClass = KtPsiUtil.getOutermostClassOrObject(classOrObject) ?:
                         throw IllegalStateException("Attempt to build a light class for a local class: " + classOrObject.text)

    return outermostClass
}

interface LightClassInheritanceHelper {
    fun isInheritor(
            lightClass: KtLightClass,
            baseClass: PsiClass,
            checkDeep: Boolean
    ): ImpreciseResolveResult

    object NoHelp : LightClassInheritanceHelper {
        override fun isInheritor(lightClass: KtLightClass, baseClass: PsiClass, checkDeep: Boolean) = UNSURE
    }

    companion object {
        fun getService(project: Project): LightClassInheritanceHelper =
                ServiceManager.getService(project, LightClassInheritanceHelper::class.java) ?: NoHelp
    }
}

fun KtClassOrObject.defaultJavaAncestorQualifiedName(): String? {
    if (this !is KtClass) return CommonClassNames.JAVA_LANG_OBJECT

    return when {
        isAnnotation() -> CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION
        isEnum() -> CommonClassNames.JAVA_LANG_ENUM
        isInterface() -> CommonClassNames.JAVA_LANG_OBJECT // see com.intellij.psi.impl.PsiClassImplUtil.getSuperClass
        else -> CommonClassNames.JAVA_LANG_OBJECT
    }
}
