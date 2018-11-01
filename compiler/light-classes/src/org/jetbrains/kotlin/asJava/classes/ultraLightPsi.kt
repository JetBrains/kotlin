/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.google.common.annotations.VisibleForTesting
import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.*
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.annotations.STRICTFP_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.SYNCHRONIZED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

class KtUltraLightClass(classOrObject: KtClassOrObject, private val support: UltraLightSupport) :
    KtLightClassImpl(classOrObject) {
    companion object {

        // This property may be removed once IntelliJ versions earlier than 2018.3 become unsupported
        // And usages of that property may be replaced with relevant registry change
        @Volatile
        @TestOnly
        var forceUsingUltraLightClasses = false
    }

    private val tooComplex: Boolean by lazyPub { support.isTooComplexForUltraLightGeneration(classOrObject) }

    override fun isFinal(isFinalByPsi: Boolean) = if (tooComplex) super.isFinal(isFinalByPsi) else isFinalByPsi

    @Volatile
    @VisibleForTesting
    var isClsDelegateLoaded = false

    override fun findLightClassData(): LightClassData = super.findLightClassData().also {
        if (!isClsDelegateLoaded) {
            isClsDelegateLoaded = true
            check(tooComplex) {
                "Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: $qualifiedName"
            }
        }
    }

    private fun allSuperTypes() =
        getDescriptor()?.typeConstructor?.supertypes.orEmpty().asSequence()

    private fun mapSupertype(supertype: KotlinType) =
        supertype.asPsiType(classOrObject, support, TypeMappingMode.SUPER_TYPE, this) as? PsiClassType

    override fun createExtendsList(): PsiReferenceList? =
        if (tooComplex) super.createExtendsList()
        else KotlinLightReferenceListBuilder(
            manager,
            language,
            PsiReferenceList.Role.EXTENDS_LIST
        ).also { list ->
            allSuperTypes()
                .filter { (isInterface || !JvmCodegenUtil.isJvmInterface(it)) && !it.isAnyOrNullableAny() }
                .map(this::mapSupertype)
                .forEach(list::addReference)
        }

    override fun createImplementsList(): PsiReferenceList? =
        if (tooComplex) super.createImplementsList()
        else KotlinLightReferenceListBuilder(
            manager,
            language,
            PsiReferenceList.Role.IMPLEMENTS_LIST
        ).also { list ->
            if (!isInterface) {
                allSuperTypes()
                    .filter { JvmCodegenUtil.isJvmInterface(it) }
                    .map(this::mapSupertype)
                    .forEach(list::addReference)
            }
        }

    override fun buildTypeParameterList(): PsiTypeParameterList =
        if (tooComplex) super.buildTypeParameterList() else buildTypeParameterList(classOrObject, this, support)

    // the following logic should be in the platform (super), overrides can be removed once that happens
    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)

    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)
    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun getRBrace(): PsiElement? = null
    override fun getLBrace(): PsiElement? = null

    private val _ownFields: List<KtLightField> by lazyPub {
        val result = arrayListOf<KtLightField>()
        val usedNames = hashSetOf<String>()

        fun generateUniqueName(base: String): String {
            if (usedNames.add(base)) return base
            var i = 1
            while (true) {
                val suggestion = "$base$$i"
                if (usedNames.add(suggestion)) return suggestion
                i++
            }
        }


        for (parameter in propertyParameters()) {
            val modifiers = hashSetOf<String>()
            modifiers.add(PsiModifier.PRIVATE)
            if (!parameter.isMutable) {
                modifiers.add(PsiModifier.FINAL)
            }
            result.add(KtUltraLightField(parameter, generateUniqueName(parameter.name.orEmpty()), this, support, modifiers))
        }

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            result.add(
                KtUltraLightField(
                    companion,
                    generateUniqueName(companion.name.orEmpty()),
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                )
            )

            for (property in companion.declarations.filterIsInstance<KtProperty>()) {
                if (isInterface && !property.hasModifier(CONST_KEYWORD)) continue

                propertyField(property, ::generateUniqueName, true)?.let(result::add)
            }
        }

        if (!isInterface &&
            !(this.classOrObject is KtObjectDeclaration && this.classOrObject.isCompanion() && containingClass?.isInterface == false)
        ) {
            for (property in this.classOrObject.declarations.filterIsInstance<KtProperty>()) {
                propertyField(property, ::generateUniqueName, forceStatic = this.classOrObject is KtObjectDeclaration)?.let(result::add)
            }
        }

        if (isNamedObject()) {
            result.add(
                KtUltraLightField(
                    this.classOrObject,
                    JvmAbi.INSTANCE_FIELD,
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                )
            )
        }

        result
    }

    private fun isNamedObject() = classOrObject is KtObjectDeclaration && !classOrObject.isCompanion()

    private fun propertyField(property: KtProperty, generateUniqueName: (String) -> String, forceStatic: Boolean): KtLightField? {
        if (!hasBackingField(property)) return null

        val hasDelegate = property.hasDelegate()
        val fieldName = generateUniqueName((property.name ?: "") + (if (hasDelegate) "\$delegate" else ""))

        val visibility = when {
            property.hasModifier(PRIVATE_KEYWORD) -> PsiModifier.PRIVATE
            property.hasModifier(LATEINIT_KEYWORD) -> {
                val declaration = property.setter ?: property
                simpleVisibility(declaration)
            }
            property.hasModifier(CONST_KEYWORD) -> PsiModifier.PUBLIC
            else -> PsiModifier.PRIVATE
        }
        val modifiers = hashSetOf(visibility)

        if (!property.isVar || property.hasModifier(CONST_KEYWORD) || hasDelegate) {
            modifiers.add(PsiModifier.FINAL)
        }

        if (forceStatic || isNamedObject() && isJvmStatic(property)) {
            modifiers.add(PsiModifier.STATIC)
        }

        return KtUltraLightField(property, fieldName, this, support, modifiers)
    }

    private fun hasBackingField(property: KtProperty): Boolean {
        if (property.hasModifier(ABSTRACT_KEYWORD)) return false
        if (property.hasModifier(LATEINIT_KEYWORD) || property.accessors.isEmpty()) return true

        val context = LightClassGenerationSupport.getInstance(project).analyze(property)
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
        return descriptor is PropertyDescriptor && context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
    }

    override fun getOwnFields(): List<KtLightField> = if (tooComplex) super.getOwnFields() else _ownFields

    private fun propertyParameters() = classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() }

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = arrayListOf<KtLightMethod>()
        for (declaration in this.classOrObject.declarations.filterNot { isHiddenByDeprecation(it) }) {
            if (declaration.hasModifier(PRIVATE_KEYWORD) && isInterface) continue
            when (declaration) {
                is KtNamedFunction -> result.add(asJavaMethod(declaration, false))
                is KtProperty -> result.addAll(propertyAccessors(declaration, declaration.isVar, false))
            }
        }
        for (parameter in propertyParameters()) {
            result.addAll(propertyAccessors(parameter, parameter.isMutable, false))
        }
        if (!isInterface) {
            result.addAll(createConstructors())
        }
        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            for (declaration in companion.declarations.filterNot { isHiddenByDeprecation(it) }) {
                when (declaration) {
                    is KtNamedFunction -> if (isJvmStatic(declaration)) result.add(asJavaMethod(declaration, true))
                    is KtProperty -> result.addAll(propertyAccessors(declaration, declaration.isVar, true))
                }
            }
        }
        result
    }

    private fun createConstructors(): List<KtLightMethod> {
        val result = arrayListOf<KtLightMethod>()
        val constructors = classOrObject.allConstructors
        if (constructors.isEmpty()) {
            result.add(defaultConstructor())
        }
        for (constructor in constructors.filterNot { isHiddenByDeprecation(it) }) {
            result.add(asJavaMethod(constructor, false))
        }
        val primary = classOrObject.primaryConstructor
        if (primary != null && shouldGenerateNoArgOverload(primary)) {
            result.add(noArgConstructor(simpleVisibility(primary), primary))
        }
        return result
    }

    private fun shouldGenerateNoArgOverload(primary: KtPrimaryConstructor): Boolean {
        return !primary.hasModifier(PRIVATE_KEYWORD) &&
                !classOrObject.hasModifier(INNER_KEYWORD) &&
                primary.valueParameters.isNotEmpty() &&
                primary.valueParameters.all { it.defaultValue != null } &&
                classOrObject.allConstructors.none { it.valueParameters.isEmpty() }
    }

    private fun defaultConstructor(): KtUltraLightMethod {
        val visibility =
            if (classOrObject is KtObjectDeclaration || classOrObject.hasModifier(SEALED_KEYWORD)) PsiModifier.PRIVATE
            else PsiModifier.PUBLIC
        return noArgConstructor(visibility, classOrObject)
    }

    private fun simpleVisibility(declaration: KtDeclaration): String = when {
        declaration.hasModifier(PRIVATE_KEYWORD) -> PsiModifier.PRIVATE
        declaration.hasModifier(PROTECTED_KEYWORD) -> PsiModifier.PROTECTED
        else -> PsiModifier.PUBLIC
    }

    private fun noArgConstructor(visibility: String, declaration: KtDeclaration): KtUltraLightMethod = KtUltraLightMethod(
        LightMethodBuilder(manager, language, name.orEmpty()).setConstructor(true).addModifier(visibility),
        declaration,
        support,
        this
    )

    private fun isHiddenByDeprecation(declaration: KtDeclaration): Boolean {
        val deprecated = support.findAnnotation(declaration, FqName("kotlin.Deprecated"))?.second
        return (deprecated?.argumentValue("level") as? EnumValue)?.enumEntryName?.asString() == "HIDDEN"
    }

    private fun isJvmStatic(declaration: KtAnnotated): Boolean = declaration.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)

    override fun getOwnMethods(): List<KtLightMethod> = if (tooComplex) super.getOwnMethods() else _ownMethods

    private fun asJavaMethod(f: KtFunction, forceStatic: Boolean): KtLightMethod {
        val isConstructor = f is KtConstructor<*>
        val name = if (isConstructor) this.name else mangleIfNeeded(listOf(f), f.name ?: SpecialNames.NO_NAME_PROVIDED.asString())
        val method = lightMethod(name.orEmpty(), f, forceStatic)
        val wrapper = KtUltraLightMethod(method, f, support, this)
        addReceiverParameter(f, wrapper)
        for (parameter in f.valueParameters) {
            method.addParameter(KtUltraLightParameter(parameter.name.orEmpty(), parameter, support, wrapper, null))
        }
        val returnType: PsiType? by lazyPub {
            if (isConstructor) null
            else methodReturnType(f, wrapper)
        }
        method.setMethodReturnType { returnType }
        return wrapper
    }

    private fun addReceiverParameter(f: KtDeclaration, method: KtUltraLightMethod) {
        val receiver = (f as? KtCallableDeclaration)?.receiverTypeReference
        if (receiver != null) {
            method.delegate.addParameter(KtUltraLightParameter("\$self", f, support, method, receiver))
        }
    }

    private fun methodReturnType(f: KtDeclaration, wrapper: KtUltraLightMethod): PsiType {
        val desc = f.resolve()?.let { if (it is PropertyDescriptor) it.getter else it }
        val kotlinType = (desc as? FunctionDescriptor)?.returnType ?: return PsiType.NULL
        val mode = when {
            typeMapper(support).forceBoxedReturnType(desc) -> TypeMappingMode.RETURN_TYPE_BOXED
            else -> TypeMappingMode.getOptimalModeForReturnType(kotlinType, false)
        }
        return kotlinType.asPsiType(f, support, mode, wrapper)
    }

    private fun lightMethod(name: String, declaration: KtDeclaration, forceStatic: Boolean): LightMethodBuilder {
        val accessedProperty = if (declaration is KtPropertyAccessor) declaration.property else null
        val outer = accessedProperty ?: declaration
        return LightMethodBuilder(
            manager, language, name,
            LightParameterListBuilder(manager, language),
            object : LightModifierList(manager, language) {
                override fun hasModifierProperty(name: String): Boolean {
                    if (name == PsiModifier.PUBLIC || name == PsiModifier.PROTECTED || name == PsiModifier.PRIVATE) {
                        if (declaration.isPrivate() || accessedProperty?.isPrivate() == true) {
                            return name == PsiModifier.PRIVATE
                        }
                        if (declaration.hasModifier(PROTECTED_KEYWORD) || accessedProperty?.hasModifier(PROTECTED_KEYWORD) == true) {
                            return name == PsiModifier.PROTECTED
                        }

                        if (outer.hasModifier(OVERRIDE_KEYWORD)) {
                            when ((outer.resolve() as? CallableDescriptor)?.visibility) {
                                Visibilities.PUBLIC -> return name == PsiModifier.PUBLIC
                                Visibilities.PRIVATE -> return name == PsiModifier.PRIVATE
                                Visibilities.PROTECTED -> return name == PsiModifier.PROTECTED
                            }
                        }

                        return name == PsiModifier.PUBLIC
                    }

                    return when (name) {
                        PsiModifier.FINAL -> !isInterface && outer !is KtConstructor<*> && isFinal(outer)
                        PsiModifier.ABSTRACT -> isInterface || outer.hasModifier(ABSTRACT_KEYWORD)
                        PsiModifier.STATIC -> forceStatic || isNamedObject() && isJvmStatic(outer)
                        PsiModifier.STRICTFP -> declaration is KtFunction && declaration.hasAnnotation(STRICTFP_ANNOTATION_FQ_NAME)
                        PsiModifier.SYNCHRONIZED -> declaration is KtFunction && declaration.hasAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)
                        else -> false
                    }
                }

                fun KtDeclaration.isPrivate() =
                    hasModifier(PRIVATE_KEYWORD) ||
                            this is KtConstructor<*> && classOrObject.hasModifier(SEALED_KEYWORD) ||
                            isInlineOnly()

                private fun KtDeclaration.isInlineOnly(): Boolean {
                    if (this !is KtCallableDeclaration || !hasModifier(INLINE_KEYWORD)) return false
                    if (typeParameters.any { it.hasModifier(REIFIED_KEYWORD) }) return true
                    if (annotationEntries.isEmpty()) return false

                    val descriptor = resolve() as? CallableMemberDescriptor ?: return false

                    return descriptor.isInlineOnly()
                }
            }
        ).setConstructor(declaration is KtConstructor<*>)
    }

    private fun mangleIfNeeded(declarations: List<KtDeclaration>, name: String): String {
        for (declaration in declarations) {
            if (declaration.hasModifier(PRIVATE_KEYWORD) ||
                declaration.hasModifier(PROTECTED_KEYWORD) ||
                declaration.hasModifier(PUBLIC_KEYWORD)
            ) {
                return name
            }
            if (isInternal(declaration) && declaration.resolve()?.isPublishedApi() != true) {
                return KotlinTypeMapper.InternalNameMapper.mangleInternalName(name, support.moduleName)
            }
        }
        return name
    }

    private fun KtAnnotated.hasAnnotation(name: FqName) = support.findAnnotation(this, name) != null

    private fun isInternal(f: KtDeclaration): Boolean {
        if (f.hasModifier(OVERRIDE_KEYWORD)) {
            val desc = f.resolve()
            return desc is CallableDescriptor &&
                    desc.visibility.effectiveVisibility(desc, false) == EffectiveVisibility.Internal
        }
        return f.hasModifier(INTERNAL_KEYWORD)
    }

    private fun propertyAccessors(declaration: KtNamedDeclaration, mutable: Boolean, onlyJvmStatic: Boolean): List<KtLightMethod> {
        val propertyName = declaration.name
        if (declaration.hasModifier(CONST_KEYWORD) || propertyName == null) return emptyList()

        val ktGetter = (declaration as? KtProperty)?.getter
        val ktSetter = (declaration as? KtProperty)?.setter

        val isPrivate = declaration.hasModifier(PRIVATE_KEYWORD)
        if (isPrivate && declaration !is KtProperty) return emptyList()

        fun needsAccessor(accessor: KtPropertyAccessor?): Boolean {
            if (!onlyJvmStatic || isJvmStatic(declaration) || accessor != null && isJvmStatic(accessor)) {
                if (declaration is KtProperty && declaration.hasDelegate()) {
                    return true
                }
                if (accessor?.hasModifier(PRIVATE_KEYWORD) == true) {
                    return false
                }
                if (!isPrivate || accessor?.hasBody() == true) {
                    return true
                }
            }
            return false
        }

        val result = arrayListOf<KtLightMethod>()

        if (needsAccessor(ktGetter)) {
            val getterName = mangleIfNeeded(listOfNotNull(ktGetter, declaration), JvmAbi.getterName(propertyName))
            val getterPrototype = lightMethod(getterName, ktGetter ?: declaration, onlyJvmStatic)
            val getterWrapper = KtUltraLightMethod(getterPrototype, declaration, support, this)
            val getterType: PsiType by lazyPub { methodReturnType(declaration, getterWrapper) }
            getterPrototype.setMethodReturnType { getterType }
            addReceiverParameter(declaration, getterWrapper)
            result.add(getterWrapper)
        }

        if (mutable && needsAccessor(ktSetter)) {
            val setterName = mangleIfNeeded(listOfNotNull(ktSetter, declaration), JvmAbi.setterName(propertyName))
            val setterPrototype = lightMethod(setterName, ktSetter ?: declaration, onlyJvmStatic)
                .setMethodReturnType(PsiType.VOID)
            val setterWrapper = KtUltraLightMethod(setterPrototype, declaration, support, this)
            addReceiverParameter(declaration, setterWrapper)
            val parameterOrigin = ktSetter?.parameter ?: declaration
            setterPrototype.addParameter(KtUltraLightParameter(propertyName, parameterOrigin, support, setterWrapper, null))
            result.add(setterWrapper)
        }
        return result
    }

    private fun isFinal(declaration: KtDeclaration): Boolean {
        if (declaration.hasModifier(FINAL_KEYWORD)) return true
        return declaration !is KtPropertyAccessor &&
                !declaration.hasModifier(OPEN_KEYWORD) &&
                !declaration.hasModifier(OVERRIDE_KEYWORD) &&
                !declaration.hasModifier(ABSTRACT_KEYWORD)
    }

    override fun getInitializers(): Array<PsiClassInitializer> = emptyArray()

    override fun getContainingClass(): PsiClass? =
        if (tooComplex) super.getContainingClass() else classOrObject.containingClass()?.let(KtLightClassForSourceDeclaration::create)

    override fun getParent(): PsiElement? = if (tooComplex) super.getParent() else containingClass ?: containingFile

    override fun getScope(): PsiElement? = if (tooComplex) super.getScope() else parent
    override fun copy(): KtLightClassImpl = KtUltraLightClass(classOrObject.copy() as KtClassOrObject, support)
}

private class KtUltraLightField(
    private val declaration: KtNamedDeclaration,
    name: String,
    private val containingClass: KtUltraLightClass,
    private val support: UltraLightSupport,
    modifiers: Set<String>
) : LightFieldBuilder(name, PsiType.NULL, declaration), KtLightField {
    private val modList = object : KtLightSimpleModifierList(this, modifiers) {
        override fun hasModifierProperty(name: String): Boolean = when (name) {
            PsiModifier.VOLATILE -> hasFieldAnnotation(VOLATILE_ANNOTATION_FQ_NAME)
            PsiModifier.TRANSIENT -> hasFieldAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)
            else -> super.hasModifierProperty(name)
        }

        private fun hasFieldAnnotation(fqName: FqName): Boolean {
            val annotation = support.findAnnotation(declaration, fqName)?.first ?: return false
            val target = annotation.useSiteTarget?.getAnnotationUseSiteTarget() ?: return true
            val expectedTarget =
                if (declaration is KtProperty && declaration.hasDelegate()) AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
                else AnnotationUseSiteTarget.FIELD
            return target == expectedTarget
        }
    }

    override fun getModifierList(): PsiModifierList = modList
    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name) //can be removed after IDEA platform does the same

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    private val _type: PsiType by lazyPub {
        fun nonExistent() = JavaPsiFacade.getElementFactory(project).createTypeFromText("error.NonExistentClass", declaration)
        when {
            declaration is KtProperty && declaration.hasDelegate() ->
                (declaration.resolve() as? PropertyDescriptor)
                    ?.let {
                        val context = LightClassGenerationSupport.getInstance(project).analyze(declaration)
                        PropertyCodegen.getDelegateTypeForProperty(declaration, it, context)
                    }
                    ?.let { it.asPsiType(declaration, support, TypeMappingMode.getOptimalModeForValueParameter(it), this) }
                    ?.let(TypeConversionUtil::erasure)
                    ?: nonExistent()
            declaration is KtObjectDeclaration ->
                KtLightClassForSourceDeclaration.create(declaration)?.let { JavaPsiFacade.getElementFactory(project).createType(it) }
                    ?: nonExistent()
            else ->
                declaration.getKotlinType()?.let {
                    val mode = when {
                        (declaration.resolve() as? PropertyDescriptor)?.isVar == true -> TypeMappingMode.getOptimalModeForValueParameter(it)
                        else -> TypeMappingMode.getOptimalModeForReturnType(it, false)
                    }
                    it.asPsiType(declaration, support, mode, this)
                } ?: PsiType.NULL
        }
    }

    override fun getType(): PsiType = _type

    override fun getParent() = containingClass
    override fun getContainingClass() = containingClass
    override fun getContainingFile(): PsiFile? = containingClass.containingFile

    override fun computeConstantValue(): Any? =
        if (hasModifierProperty(PsiModifier.FINAL) &&
            (TypeConversionUtil.isPrimitiveAndNotNull(_type) || _type.equalsToText(CommonClassNames.JAVA_LANG_STRING))
        )
            (declaration.resolve() as? VariableDescriptor)?.compileTimeInitializer?.value
        else null

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? = computeConstantValue()

    override val kotlinOrigin = declaration
    override val clsDelegate: PsiField
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")
    override val lightMemberOrigin = LightMemberOriginForDeclaration(declaration, JvmDeclarationOriginKind.OTHER)

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    override fun setInitializer(initializer: PsiExpression?) = cannotModify()

}

internal class KtUltraLightMethod(
    internal val delegate: LightMethodBuilder,
    originalElement: KtDeclaration,
    private val support: UltraLightSupport,
    containingClass: KtUltraLightClass
) : KtLightMethodImpl({ delegate }, LightMemberOriginForDeclaration(originalElement, JvmDeclarationOriginKind.OTHER), containingClass) {

    // These two overrides are necessary because ones from KtLightMethodImpl suppose that clsDelegate.returnTypeElement is valid
    // While here we only set return type for LightMethodBuilder (see org.jetbrains.kotlin.asJava.classes.KtUltraLightClass.asJavaMethod)
    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun getReturnType(): PsiType? = clsDelegate.returnType

    override fun buildParametersForList(): List<PsiParameter> = clsDelegate.parameterList.parameters.toList()

    // should be in super
    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    override fun buildTypeParameterList(): PsiTypeParameterList {
        val origin = kotlinOrigin
        return if (origin is KtFunction || origin is KtProperty)
            buildTypeParameterList(origin as KtTypeParameterListOwner, this, support)
        else LightTypeParameterListBuilder(manager, language)
    }

    private val _throwsList: PsiReferenceList by lazyPub {
        val list = KotlinLightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST)
        (kotlinOrigin?.resolve() as? FunctionDescriptor)?.let {
            for (ex in FunctionCodegen.getThrownExceptions(it)) {
                list.addReference(ex.fqNameSafe.asString())
            }
        }
        list
    }

    override fun getThrowsList(): PsiReferenceList = _throwsList
}

internal class KtUltraLightParameter(
    name: String,
    override val kotlinOrigin: KtDeclaration,
    private val support: UltraLightSupport,
    method: KtLightMethod,
    private val receiver: KtTypeReference?
) : org.jetbrains.kotlin.asJava.elements.LightParameter(
    name,
    PsiType.NULL,
    method,
    method.language
),
    KtLightDeclaration<KtDeclaration, PsiParameter> {

    override val clsDelegate: PsiParameter
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")

    private val lightModifierList by lazyPub { KtLightSimpleModifierList(this, emptySet()) }

    override fun isVarArgs(): Boolean =
        kotlinOrigin is KtParameter && kotlinOrigin.isVarArg && method.parameterList.parameters.last() == this

    override fun getModifierList(): PsiModifierList = lightModifierList

    override fun getNavigationElement(): PsiElement = kotlinOrigin

    private val kotlinType: KotlinType? by lazyPub {
        when {
            receiver != null -> (kotlinOrigin.resolve() as? CallableMemberDescriptor)?.extensionReceiverParameter?.type
            else -> kotlinOrigin.getKotlinType()
        }
    }
    private val _type: PsiType by lazyPub {
        kotlinType?.let { it.asPsiType(kotlinOrigin, support, TypeMappingMode.getOptimalModeForValueParameter(it), this) } ?: PsiType.NULL
    }

    override fun getType(): PsiType = _type

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtVariableDeclaration)?.setName(name)
        return this
    }

    override fun getContainingFile(): PsiFile = method.containingFile
    override fun getParent(): PsiElement = method.parameterList

    override fun equals(other: Any?): Boolean = other is KtUltraLightParameter && other.kotlinOrigin == this.kotlinOrigin
    override fun hashCode(): Int = kotlinOrigin.hashCode()

    internal fun annotatedOrigin(): KtAnnotated? {
        if (receiver != null) return receiver

        if (kotlinOrigin is KtProperty) {
            return null // we're a setter of a property with no explicit declaration, so we don't have annotation
        }
        return kotlinOrigin
    }

    internal fun getTypeForNullability(): KotlinType? {
        if (receiver != null) return kotlinType
        if (kotlinOrigin is KtProperty) {
            if (kotlinOrigin.setter?.hasModifier(PRIVATE_KEYWORD) == true) return null
            return kotlinType
        }
        if (kotlinOrigin is KtParameter) {
            val reference = kotlinOrigin.typeReference
            if (kotlinOrigin.isVarArg && reference != null) {
                LightClassGenerationSupport.getInstance(project).analyze(reference)[BindingContext.TYPE, reference]?.let { return it }
            }
            if (reference != null || kotlinOrigin.parent?.parent is KtPropertyAccessor) {
                return kotlinType
            }
        }
        return null
    }

}

interface UltraLightSupport {
    val moduleName: String
    fun findAnnotation(owner: KtAnnotated, fqName: FqName): Pair<KtAnnotationEntry, AnnotationDescriptor>?
    fun isTooComplexForUltraLightGeneration(element: KtClassOrObject): Boolean
}
