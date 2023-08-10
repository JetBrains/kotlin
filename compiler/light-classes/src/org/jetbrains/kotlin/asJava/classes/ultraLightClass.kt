/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtUltraLightModifierList
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_RECORD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegationResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

open class KtUltraLightClass(classOrObject: KtClassOrObject, internal val support: KtUltraLightSupport) :
    KtLightClassImpl(classOrObject, support.jvmDefaultMode) {

    private class KtUltraLightClassModifierList(
        private val containingClass: KtLightClassForSourceDeclaration,
        support: KtUltraLightSupport,
        private val lazyModifiers: Lazy<Set<String>>,
        private val lazyIsFinal: Lazy<Boolean>,
    ) : KtUltraLightModifierList<KtLightClassForSourceDeclaration>(containingClass, support) {
        override fun hasModifierProperty(name: String): Boolean =
            if (name != PsiModifier.FINAL) name in lazyModifiers.value else owner.isFinal(lazyIsFinal.value)

        override fun copy(): PsiElement = KtUltraLightClassModifierList(containingClass, support, lazyModifiers, lazyIsFinal)
    }

    private val membersBuilder by lazyPub {
        UltraLightMembersCreator(
            this,
            isNamedObject(),
            classOrObject.hasModifier(SEALED_KEYWORD),
            mangleInternalFunctions = true,
            support = support,
        )
    }

    private val _deprecated by lazyPub { classOrObject.isDeprecated(support) }

    override fun isFinal(isFinalByPsi: Boolean) = isFinalByPsi

    private val _modifierList: PsiModifierList? by lazyPub {
        KtUltraLightClassModifierList(
            containingClass = this,
            support = support,
            lazyModifiers = lazyPub { computeModifiersByPsi() },
            lazyIsFinal = lazyPub { computeIsFinal() },
        )
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    private fun allSuperTypes() =
        getDescriptor()?.typeConstructor?.supertypes.orEmpty()

    private fun mapSupertype(supertype: KotlinType, kotlinCollectionAsIs: Boolean = false) =
        supertype.asPsiType(
            support,
            if (kotlinCollectionAsIs) TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS else TypeMappingMode.SUPER_TYPE,
            this
        ) as? PsiClassType

    override fun createExtendsList(): PsiReferenceList? = createInheritanceList(forExtendsList = true)

    override fun createImplementsList(): PsiReferenceList? = createInheritanceList(forExtendsList = false)

    private fun createInheritanceList(forExtendsList: Boolean): PsiReferenceList {

        val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

        if (isAnnotationType) return KotlinLightReferenceListBuilder(manager, language, role)

        val superTypes = allSuperTypes().filter {
            isTypeForInheritanceList(it, forExtendsList)
        }

        val listBuilder = KotlinSuperTypeListBuilder(
            this,
            kotlinOrigin = kotlinOrigin.getSuperTypeList(),
            manager = manager,
            language = language,
            role = role
        )

        for (superType in superTypes) {
            addTypeToTypeList(
                listBuilder = listBuilder,
                superType = superType
            )
        }

        return listBuilder
    }

    private fun addTypeToTypeList(listBuilder: KotlinSuperTypeListBuilder, superType: KotlinType) {

        val mappedType = mapSupertype(superType, kotlinCollectionAsIs = true) ?: return

        listBuilder.addReference(mappedType)

        if (mappedType.canonicalText.startsWith("kotlin.collections.")) {

            val mappedToNoCollectionAsIs = mapSupertype(superType, kotlinCollectionAsIs = false)

            if (mappedToNoCollectionAsIs !== null &&
                mappedType.canonicalText != mappedToNoCollectionAsIs.canonicalText
            ) {
                //Add java supertype
                listBuilder.addReference(mappedToNoCollectionAsIs)
                //Add marker interface
                superType.constructor.declarationDescriptor.classId?.let { classId ->
                    listBuilder.addMarkerInterfaceIfNeeded(classId)
                }
            }
        }
    }

    private fun isTypeForInheritanceList(supertype: KotlinType, forExtendsList: Boolean): Boolean {
        // Do not add redundant "extends java.lang.Object" anywhere
        if (supertype.isAnyOrNullableAny()) return false

        // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
        if (isEnum && KotlinBuiltIns.isEnum(supertype)) return false

        // Interfaces have only extends lists
        if (isInterface) return forExtendsList

        return forExtendsList == !JvmCodegenUtil.isJvmInterface(supertype)
    }

    override fun buildTypeParameterList(): PsiTypeParameterList = buildTypeParameterListForSourceDeclaration(classOrObject, this, support)

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

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            result.add(
                KtUltraLightFieldForSourceDeclaration(
                    companion,
                    membersBuilder.generateUniqueFieldName(companion.name.orEmpty(), usedNames),
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, companion.simpleVisibility())
                )
            )

            for (property in companion.declarations.filterIsInstance<KtProperty>()) {
                if (isInterface && !property.isConstOrJvmField()) continue
                membersBuilder.createPropertyField(property, usedNames, true)?.let(result::add)
            }
        }

        fun ArrayList<KtLightField>.updateWithCompilerPlugins() = also {
            val lazyDescriptor = lazy { getDescriptor() }
            project.applyCompilerPlugins {
                it.interceptFieldsBuilding(
                    declaration = kotlinOrigin,
                    descriptor = lazyDescriptor,
                    containingDeclaration = this@KtUltraLightClass,
                    fieldsList = result
                )
            }
        }

        if (isAnnotationType) return@lazyPub result.updateWithCompilerPlugins()

        for (parameter in propertyParameters()) {
            membersBuilder.createPropertyField(parameter, usedNames, forceStatic = false)?.let(result::add)
        }

        if (!isInterface) {
            val isCompanion = this.classOrObject.safeAs<KtObjectDeclaration>()?.isCompanion() == true
            for (property in this.classOrObject.declarations.filterIsInstance<KtProperty>()) {
                // All fields for companion object of classes are generated to the containing class
                // For interfaces, only @JvmField-annotated properties are generated to the containing class
                // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
                if (isCompanion && (containingClass?.isInterface == false || property.isJvmField())) continue

                membersBuilder.createPropertyField(property, usedNames, forceStatic = this.classOrObject is KtObjectDeclaration)
                    ?.let(result::add)
            }
        }

        if (isNamedObject() && !this.classOrObject.isLocal) {
            result.add(
                KtUltraLightFieldForSourceDeclaration(
                    this.classOrObject,
                    JvmAbi.INSTANCE_FIELD,
                    this,
                    support,
                    setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                )
            )
        }

        if (isEnum) {
            for (ktEnumEntry in classOrObject.declarations.filterIsInstance<KtEnumEntry>()) {
                val name = ktEnumEntry.name ?: continue
                result.add(
                    KtUltraLightEnumEntry(
                        ktEnumEntry, name, this, support,
                        setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC)
                    )
                )
            }
        }

        result.updateWithCompilerPlugins()
    }

    private fun isNamedObject() = classOrObject is KtObjectDeclaration && !classOrObject.cast<KtObjectDeclaration>().isCompanion()

    override fun getOwnFields(): List<KtLightField> = _ownFields

    private fun propertyParameters() = classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() }

    private fun ownMethods(): List<PsiMethod> {
        val result = mutableListOf<PsiMethod>()

        for (declaration in this.classOrObject.declarations.filterNot { it.isHiddenByDeprecation(support) }) {
            if (declaration.hasModifier(PRIVATE_KEYWORD) && isInterface) continue
            when (declaration) {
                is KtNamedFunction -> result.addAll(membersBuilder.createMethods(declaration, forceStatic = false))
                is KtProperty -> result.addAll(
                    membersBuilder.propertyAccessors(declaration, declaration.isVar, forceStatic = false, onlyJvmStatic = false)
                )
            }
        }

        for (parameter in propertyParameters()) {
            result.addAll(
                membersBuilder.propertyAccessors(
                    parameter,
                    parameter.isMutable,
                    forceStatic = false,
                    onlyJvmStatic = false,
                    createAsAnnotationMethod = isAnnotationType,
                    isJvmRecord = classOrObject.hasAnnotation(JVM_RECORD_ANNOTATION_FQ_NAME),
                )
            )
        }

        if (!isInterface) {
            result.addAll(createConstructors())
        }

        this.classOrObject.companionObjects.firstOrNull()?.let { companion ->
            for (declaration in companion.declarations.filterNot { isHiddenByDeprecation(it) }) {
                when (declaration) {
                    is KtNamedFunction ->
                        if (isJvmStatic(declaration)) result.addAll(membersBuilder.createMethods(declaration, forceStatic = true))

                    is KtProperty -> result.addAll(
                        membersBuilder.propertyAccessors(
                            declaration,
                            declaration.isVar,
                            forceStatic = false,
                            onlyJvmStatic = true
                        )
                    )
                }
            }
        }

        addMethodsFromDataClass(result)
        addDelegatesToInterfaceMethods(result)

        val lazyDescriptor = lazy { getDescriptor() }
        project.applyCompilerPlugins {
            val methodsList = mutableListOf<KtLightMethod>()
            it.interceptMethodsBuilding(
                declaration = kotlinOrigin,
                descriptor = lazyDescriptor,
                containingDeclaration = this,
                methodsList = methodsList
            )
            result.addAll(methodsList)
        }
        if (isEnum && support.languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)) {
            result.add(getEnumEntriesPsiMethod(this))
        }

        return result
    }

    private val _ownMethods: CachedValue<List<PsiMethod>> = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result.create(
                ownMethods(),
                classOrObject.getExternalDependencies()
            )
        }, false
    )

    private fun addMethodsFromDataClass(result: MutableList<PsiMethod>) {
        if (!classOrObject.hasModifier(DATA_KEYWORD)) return
        val ktClass = classOrObject as? KtClass ?: return
        val descriptor = classOrObject.resolve() as? ClassDescriptor ?: return
        val bindingContext = classOrObject.analyze()

        // Force resolving data class members set
        descriptor.unsubstitutedMemberScope.getContributedDescriptors()

        val areCtorParametersAreAnalyzed = ktClass.primaryConstructorParameters
            .filter { it.hasValOrVar() }
            .all { bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it) != null }

        if (!areCtorParametersAreAnalyzed) return

        object : DataClassMethodGenerator(classOrObject, bindingContext) {
            private fun addFunction(descriptor: FunctionDescriptor, declarationForOrigin: KtDeclaration? = null) {
                result.add(createGeneratedMethodFromDescriptor(descriptor, JvmDeclarationOriginKind.OTHER, declarationForOrigin))
            }

            override fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor) {
                addFunction(function, DescriptorToSourceUtils.descriptorToDeclaration(parameter) as? KtDeclaration)
            }

            override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
                addFunction(function)
            }

            override fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
                addFunction(function)
            }

            override fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
                addFunction(function)
            }

            override fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
                addFunction(function)
            }
        }.generate()
    }

    private fun addDelegatesToInterfaceMethods(result: MutableList<PsiMethod>) {
        classOrObject.superTypeListEntries.filterIsInstance<KtDelegatedSuperTypeEntry>().forEach {
            addDelegatesToInterfaceMethods(it, result)
        }
    }

    private fun addDelegatesToInterfaceMethods(
        superTypeEntry: KtDelegatedSuperTypeEntry,
        result: MutableList<PsiMethod>
    ) {
        val classDescriptor = classOrObject.resolve() as? ClassDescriptor ?: return
        val typeReference = superTypeEntry.typeReference ?: return
        val bindingContext = typeReference.analyze()

        val superClassDescriptor = CodegenUtil.getSuperClassBySuperTypeListEntry(superTypeEntry, bindingContext) ?: return
        val delegationType = superTypeEntry.delegateExpression.kotlinType(bindingContext)

        for (delegate in DelegationResolver.getDelegates(classDescriptor, superClassDescriptor, delegationType).keys) {
            when (delegate) {

                is PropertyDescriptor -> delegate.accessors.mapTo(result) {
                    createGeneratedMethodFromDescriptor(
                        descriptor = it,
                        declarationOriginKindForOrigin = JvmDeclarationOriginKind.DELEGATION
                    )
                }

                is FunctionDescriptor -> result.add(
                    createGeneratedMethodFromDescriptor(
                        descriptor = delegate,
                        declarationOriginKindForOrigin = JvmDeclarationOriginKind.DELEGATION
                    )
                )
            }
        }
    }

    private fun createConstructors(): List<KtLightMethod> {
        val result = arrayListOf<KtLightMethod>()
        val constructors = classOrObject.allConstructors
        if (constructors.isEmpty()) {
            result.add(defaultConstructor())
        }
        for (constructor in constructors.filterNot { isHiddenByDeprecation(it) }) {
            result.addAll(membersBuilder.createMethods(constructor, false, forcePrivate = isEnum))
        }
        val primary = classOrObject.primaryConstructor
        if (primary != null && shouldGenerateNoArgOverload(primary)) {
            result.add(noArgConstructor(primary.simpleVisibility(), primary, METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR))
        }
        return result
    }

    private fun shouldGenerateNoArgOverload(primary: KtPrimaryConstructor): Boolean {
        return !primary.hasModifier(PRIVATE_KEYWORD) &&
                !classOrObject.hasModifier(INNER_KEYWORD) && !isEnum &&
                !classOrObject.hasModifier(SEALED_KEYWORD) &&
                primary.valueParameters.isNotEmpty() &&
                primary.valueParameters.all { it.defaultValue != null } &&
                classOrObject.allConstructors.none { it.valueParameters.isEmpty() } &&
                !primary.hasAnnotation(JVM_OVERLOADS_FQ_NAME)
    }

    private fun defaultConstructor(): KtUltraLightMethod {
        val visibility =
            when {
                classOrObject is KtObjectDeclaration || isEnum -> PsiModifier.PRIVATE
                classOrObject.hasModifier(SEALED_KEYWORD) -> PsiModifier.PROTECTED
                classOrObject is KtEnumEntry -> PsiModifier.PACKAGE_LOCAL
                else -> PsiModifier.PUBLIC
            }
        return noArgConstructor(visibility, classOrObject, METHOD_INDEX_FOR_DEFAULT_CTOR)
    }

    private fun noArgConstructor(
        visibility: String,
        declaration: KtDeclaration,
        methodIndex: Int
    ): KtUltraLightMethod =
        KtUltraLightMethodForSourceDeclaration(
            LightMethodBuilder(manager, language, name.orEmpty()).setConstructor(true).addModifier(visibility),
            declaration,
            support,
            this,
            methodIndex
        )

    private fun isHiddenByDeprecation(declaration: KtDeclaration): Boolean {
        val deprecated = support.findAnnotation(declaration, FqName("kotlin.Deprecated"))?.second
        return (deprecated?.argumentValue("level") as? EnumValue)?.enumEntryName?.asString() == "HIDDEN"
    }

    private fun isJvmStatic(declaration: KtAnnotated): Boolean = declaration.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods.value

    private fun KtAnnotated.hasAnnotation(name: FqName) = support.findAnnotation(this, name) != null

    private fun KtCallableDeclaration.isConstOrJvmField() =
        hasModifier(CONST_KEYWORD) || isJvmField()

    private fun KtCallableDeclaration.isJvmField() = hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

    override fun getInitializers(): Array<PsiClassInitializer> = emptyArray()

    override fun getContainingClass(): PsiClass? {

        val containingBody = classOrObject.parent as? KtClassBody
        val containingClass = containingBody?.parent as? KtClassOrObject
        containingClass?.let { return it.toLightClass() }

        val containingBlock = classOrObject.parent as? KtBlockExpression
        val containingScript = containingBlock?.parent as? KtScript
        containingScript?.let { return it.toLightClass() }

        return null
    }

    override fun getParent(): PsiElement? = containingClass ?: containingFile

    override fun getScope(): PsiElement? = parent

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean =
        baseClass?.let { InheritanceImplUtil.isInheritorDeep(this, it, classToByPass) } ?: false

    override fun isDeprecated(): Boolean = _deprecated

    override fun copy(): KtLightClassImpl = KtUltraLightClass(classOrObject.copy() as KtClassOrObject, support)

    override fun getTextRange(): TextRange? {
        if (Registry.`is`("kotlin.ultra.light.classes.empty.text.range", true)) {
            return null
        }

        return super.getTextRange()
    }

    override fun getOwnInnerClasses(): List<PsiClass> = super.getOwnInnerClasses().let { superOwnInnerClasses ->
        if (shouldGenerateRepeatableAnnotationContainer) {
            superOwnInnerClasses + KtUltraLightClassForRepeatableAnnotationContainer(classOrObject, support)
        } else
            superOwnInnerClasses
    }

    override fun createClassForInterfaceDefaultImpls(): PsiClass = KtUltraLightClassForInterfaceDefaultImpls(classOrObject, support)

    private val shouldGenerateRepeatableAnnotationContainer: Boolean
        get() = isAnnotationType &&
                classOrObject.hasAnnotation(StandardNames.FqNames.repeatable) &&
                !classOrObject.hasAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION)
}
