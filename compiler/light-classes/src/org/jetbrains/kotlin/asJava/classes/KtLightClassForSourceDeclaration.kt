/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.source.PsiImmediateClassType
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
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
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*
import javax.swing.Icon
import kotlin.jvm.Throws

private class KtLightClassModifierList(containingClass: KtLightClassForSourceDeclaration, computeModifiers: () -> Set<String>) :
    KtLightModifierList<KtLightClassForSourceDeclaration>(containingClass) {

    private val modifiers by lazyPub { computeModifiers() }

    override fun hasModifierProperty(name: String): Boolean =
        if (name != PsiModifier.FINAL) name in modifiers else owner.isFinal(PsiModifier.FINAL in modifiers)

}

abstract class KtLightClassForSourceDeclaration(
    protected val classOrObject: KtClassOrObject,
    private val forceUsingOldLightClasses: Boolean = false
) : KtLazyLightClass(classOrObject.manager),
    StubBasedPsiElement<KotlinClassOrObjectStub<out KtClassOrObject>> {

    override val myInnersCache: KotlinClassInnerStuffCache = KotlinClassInnerStuffCache(
        this,
        classOrObject.getExternalDependencies()
    )

    private val lightIdentifier = KtLightIdentifier(this, classOrObject)

    override fun getText() = kotlinOrigin.text ?: ""

    override fun getTextRange(): TextRange? = kotlinOrigin.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = kotlinOrigin.textOffset

    override fun getStartOffsetInParent() = kotlinOrigin.startOffsetInParent

    override fun isWritable() = kotlinOrigin.isWritable

    private val _extendsList by lazyPub { createExtendsList() }
    private val _implementsList by lazyPub { createImplementsList() }
    private val _deprecated by lazyPub { classOrObject.isDeprecated() }

    protected open fun createExtendsList(): PsiReferenceList? = super.getExtendsList()?.let { KtLightPsiReferenceList(it, this) }
    protected open fun createImplementsList(): PsiReferenceList? = super.getImplementsList()?.let { KtLightPsiReferenceList(it, this) }

    override val kotlinOrigin: KtClassOrObject = classOrObject

    abstract override fun copy(): PsiElement
    abstract override fun getParent(): PsiElement?
    abstract override fun getQualifiedName(): String?

    override val lightClassData: LightClassData
        get() = findLightClassData()

    protected open fun findLightClassData() = getLightClassDataHolder().findDataForClassOrObject(classOrObject)

    private fun getJavaFileStub(): PsiJavaFileStub = getLightClassDataHolder().javaFileStub

    fun getDescriptor() =
        LightClassGenerationSupport.getInstance(project).resolveToDescriptor(classOrObject) as? ClassDescriptor

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
                place: PsiElement
            ): Boolean {
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
        return kotlinOrigin.isEquivalentTo(another) ||
                another is KtLightClassForSourceDeclaration && Comparing.equal(another.qualifiedName, qualifiedName)
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

    private val _typeParameterList: PsiTypeParameterList by lazyPub { buildTypeParameterList() }

    protected open fun buildTypeParameterList() = LightClassUtil.buildLightTypeParameterList(this, classOrObject)

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList.typeParameters

    override fun getName(): String? = classOrObject.nameAsName?.asString()

    private val _modifierList: PsiModifierList by lazyPub { KtLightClassModifierList(this) { computeModifiers() } }

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
        } else if (!psiModifiers.contains(PsiModifier.PROTECTED)) {
            psiModifiers.add(PsiModifier.PUBLIC)
        }


        // FINAL
        if (isAbstract() || isSealed()) {
            psiModifiers.add(PsiModifier.ABSTRACT)
        } else if (!(classOrObject.hasModifier(OPEN_KEYWORD) || (classOrObject is KtClass && classOrObject.isEnum()))) {
            val descriptor = lazy { getDescriptor() }
            var modifier = PsiModifier.FINAL
            project.applyCompilerPlugins {
                modifier = it.interceptModalityBuilding(kotlinOrigin, descriptor, modifier)
            }
            if (modifier == PsiModifier.FINAL) {
                psiModifiers.add(PsiModifier.FINAL)
            }
        }

        if (!classOrObject.isTopLevel() && !classOrObject.hasModifier(INNER_KEYWORD)) {
            psiModifiers.add(PsiModifier.STATIC)
        }

        return psiModifiers
    }

    private fun isAbstract(): Boolean = classOrObject.hasModifier(ABSTRACT_KEYWORD) || isInterface

    private fun isSealed(): Boolean = classOrObject.hasModifier(SEALED_KEYWORD)

    override fun hasModifierProperty(@NonNls name: String): Boolean = modifierList?.hasModifierProperty(name) ?: false

    override fun isDeprecated(): Boolean = _deprecated

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
        } else {
            baseClass.qualifiedName
        }

        val thisDescriptor = getDescriptor()

        return if (qualifiedName != null && thisDescriptor != null) {
            checkSuperTypeByFQName(thisDescriptor, qualifiedName, checkDeep)
        } else {
            InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
        }
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
            .mapNotNullTo(result) {
                if (!forceUsingOldLightClasses)
                    create(it)
                else
                    createNoCache(it, forceUsingOldLightClasses = true)
            }

        if (classOrObject.hasInterfaceDefaultImpls) {
            result.add(KtLightClassForInterfaceDefaultImpls(classOrObject))
        }
        return result
    }

    override fun getUseScope(): SearchScope = kotlinOrigin.useScope

    override fun getElementType(): IStubElementType<out StubElement<*>, *>? = classOrObject.elementType
    override fun getStub(): KotlinClassOrObjectStub<out KtClassOrObject>? = classOrObject.stub

    override fun getNameIdentifier(): KtLightIdentifier? = lightIdentifier

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    companion object {
        private val JAVA_API_STUB = Key.create<CachedValue<LightClassDataHolder.ForClass>>("JAVA_API_STUB")
        private val JAVA_API_STUB_LOCK = Key.create<Any>("JAVA_API_STUB_LOCK")

        @JvmStatic
        private val javaApiStubInitIsRunning: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

        private val jetTokenToPsiModifier = listOf(
            PUBLIC_KEYWORD to PsiModifier.PUBLIC,
            INTERNAL_KEYWORD to PsiModifier.PUBLIC,
            PROTECTED_KEYWORD to PsiModifier.PROTECTED,
            FINAL_KEYWORD to PsiModifier.FINAL
        )

        fun create(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? =
            CachedValuesManager.getCachedValue(classOrObject) {
                CachedValueProvider.Result
                    .create(
                        createNoCache(classOrObject, KtUltraLightSupport.forceUsingOldLightClasses),
                        KotlinModificationTrackerService.getInstance(classOrObject.project).outOfBlockModificationTracker
                    )
            }

        fun createNoCache(classOrObject: KtClassOrObject, forceUsingOldLightClasses: Boolean): KtLightClassForSourceDeclaration? {
            val containingFile = classOrObject.containingFile
            if (containingFile is KtCodeFragment) {
                // Avoid building light classes for code fragments
                return null
            }

            if (classOrObject.shouldNotBeVisibleAsLightClass()) {
                return null
            }

            if (!forceUsingOldLightClasses && Registry.`is`("kotlin.use.ultra.light.classes", true)) {
                return LightClassGenerationSupport.getInstance(classOrObject.project).createUltraLightClass(classOrObject)
                    ?: error { "Unable to create UL class for ${classOrObject::javaClass.name}" }
            }

            return when {
                classOrObject is KtObjectDeclaration && classOrObject.isObjectLiteral() ->
                    KtLightClassForAnonymousDeclaration(classOrObject)

                classOrObject.safeIsLocal() ->
                    KtLightClassForLocalDeclaration(classOrObject)

                else ->
                    KtLightClassImpl(classOrObject, forceUsingOldLightClasses)
            }
        }

        fun getLightClassDataHolder(classOrObject: KtClassOrObject): LightClassDataHolder.ForClass {
            if (classOrObject.shouldNotBeVisibleAsLightClass()) {
                return InvalidLightClassDataHolder
            }

            val containingScript = classOrObject.containingKtFile.safeScript()
            return when {
                !classOrObject.safeIsLocal() && containingScript != null ->
                    KtLightClassForScript.getLightClassCachedValue(containingScript).value
                else ->
                    getLightClassCachedValue(classOrObject).value
            }
        }

        private fun getLightClassCachedValue(classOrObject: KtClassOrObject): CachedValue<LightClassDataHolder.ForClass> {
            val outerClassValue = getOutermostClassOrObject(classOrObject).getUserData(JAVA_API_STUB)
            outerClassValue?.let {
                // stub computed for outer class can be used for inner/nested
                return it
            }
            // the idea behind this locking approach:
            // Thread T1 starts to calculate value for A it acquires lock for A
            //
            // Assumption 1: Lets say A calculation requires another value e.g. B to be calculated
            // Assumption 2: Thread T2 wants to calculate value for B

            // to avoid dead-lock case we mark thread as doing calculation and acquire lock only once per thread
            // as a trade-off to prevent dependent value could be calculated several time
            // due to CAS (within putUserDataIfAbsent etc) the same instance of calculated value will be used
            val value: CachedValue<LightClassDataHolder.ForClass> = if (!javaApiStubInitIsRunning.get()) {
                classOrObject.getUserData(JAVA_API_STUB) ?: run {
                    val lock = classOrObject.putUserDataIfAbsent(JAVA_API_STUB_LOCK, Object())
                    synchronized(lock) {
                        try {
                            javaApiStubInitIsRunning.set(true)
                            computeLightClassCachedValue(classOrObject)
                        } finally {
                            javaApiStubInitIsRunning.set(false)
                        }
                    }
                }
            } else {
                computeLightClassCachedValue(classOrObject)
            }
            return value
        }

        private fun computeLightClassCachedValue(
            classOrObject: KtClassOrObject
        ): CachedValue<LightClassDataHolder.ForClass> {
            val value = classOrObject.getUserData(JAVA_API_STUB) ?: run {
                val manager = CachedValuesManager.getManager(classOrObject.project)
                val cachedValue = manager.createCachedValue(
                    LightClassDataProviderForClassOrObject(classOrObject), false
                )
                classOrObject.putUserDataIfAbsent(JAVA_API_STUB, cachedValue)
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
        return classOrObject.defaultJavaAncestorQualifiedName()?.let { ancestorFqName ->
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
        return PsiSubstitutor.createSubstitutor(
            mapOf(
                javaLangEnumsTypeParameter to PsiImmediateClassType(this, PsiSubstitutor.EMPTY)
            )
        )
    }

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    open fun isFinal(isFinalByPsi: Boolean): Boolean {
        // annotations can make class open via 'allopen' plugin
        if (!isPossiblyAffectedByAllOpen() || !isFinalByPsi) return isFinalByPsi

        return clsDelegate.hasModifierProperty(PsiModifier.FINAL)
    }
}

fun KtLightClassForSourceDeclaration.isPossiblyAffectedByAllOpen() =
    !isAnnotationType && !isInterface && kotlinOrigin.annotationEntries.isNotEmpty()

fun getOutermostClassOrObject(classOrObject: KtClassOrObject): KtClassOrObject {
    return KtPsiUtil.getOutermostClassOrObject(classOrObject)
        ?: throw IllegalStateException("Attempt to build a light class for a local class: " + classOrObject.text)
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

fun KtClassOrObject.shouldNotBeVisibleAsLightClass(): Boolean {
    if (parentsWithSelf.filterIsInstance<KtClassOrObject>().any { it.hasExpectModifier() }) {
        return true
    }

    if (safeIsLocal()) {
        if (containingFile.virtualFile == null) return true
        if (hasParseErrorsAround(this) || PsiUtilCore.hasErrorElementChild(this)) return true
    }

    if (isEnumEntryWithoutBody(this)) {
        return true
    }

    return false
}

private fun isEnumEntryWithoutBody(classOrObject: KtClassOrObject): Boolean {
    if (classOrObject !is KtEnumEntry) {
        return false
    }
    return classOrObject.getBody()?.declarations?.isEmpty() ?: true
}

private fun hasParseErrorsAround(psi: PsiElement): Boolean {
    val node = psi.node ?: return false

    TreeUtil.nextLeaf(node)?.let { nextLeaf ->
        if (nextLeaf.elementType == TokenType.ERROR_ELEMENT || nextLeaf.treePrev?.elementType == TokenType.ERROR_ELEMENT) {
            return true
        }
    }

    TreeUtil.prevLeaf(node)?.let { prevLeaf ->
        if (prevLeaf.elementType == TokenType.ERROR_ELEMENT || prevLeaf.treeNext?.elementType == TokenType.ERROR_ELEMENT) {
            return true
        }
    }

    return false
}