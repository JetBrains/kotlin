/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.hasInterfaceDefaultImpls
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmNameAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmOverloadsAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasJvmSyntheticAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.isHiddenOrSynthetic
import org.jetbrains.kotlin.light.classes.symbol.copy
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForProperty
import org.jetbrains.kotlin.light.classes.symbol.isJvmField
import org.jetbrains.kotlin.light.classes.symbol.mapType
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod.Companion.createPropertyAccessors
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightNoArgConstructor
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod.Companion.createSimpleMethods
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.util.*

internal fun createSymbolLightClassNoCache(classOrObject: KtClassOrObject, ktModule: KaModule): KtLightClass? = when {
    classOrObject.isObjectLiteral() -> SymbolLightClassForAnonymousObject(classOrObject, ktModule)
    classOrObject is KtEnumEntry -> lightClassForEnumEntry(classOrObject)
    else -> createLightClassNoCache(classOrObject, ktModule)
}

internal fun createLightClassNoCache(ktClassOrObject: KtClassOrObject, ktModule: KaModule): SymbolLightClassBase = when {
    ktClassOrObject.hasModifier(INLINE_KEYWORD) || ktClassOrObject.hasModifier(VALUE_KEYWORD) ->
        SymbolLightClassForValueClass(ktClassOrObject, ktModule)

    ktClassOrObject is KtClass && ktClassOrObject.isAnnotation() -> SymbolLightClassForAnnotationClass(ktClassOrObject, ktModule)
    ktClassOrObject is KtClass && ktClassOrObject.isInterface() -> SymbolLightClassForInterface(ktClassOrObject, ktModule)
    else -> SymbolLightClassForClassOrObject(ktClassOrObject, ktModule)
}

internal fun KtClassOrObject.contentModificationTrackers(): List<ModificationTracker> {
    val outOfBlockTracker = KotlinAsJavaSupportBase.getInstance(project).outOfBlockModificationTracker(this)
    return if (isLocal) {
        val file = containingKtFile
        listOf(outOfBlockTracker, ModificationTracker { file.modificationStamp })
    } else {
        listOf(outOfBlockTracker)
    }
}

internal fun KaSession.createLightClassNoCache(
    classSymbol: KaNamedClassSymbol,
    ktModule: KaModule,
    manager: PsiManager,
): SymbolLightClassBase = when (classSymbol.classKind) {
    KaClassKind.INTERFACE -> SymbolLightClassForInterface(
        ktAnalysisSession = this,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    )

    KaClassKind.ANNOTATION_CLASS -> SymbolLightClassForAnnotationClass(
        ktAnalysisSession = this,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    )

    else -> if (classSymbol.isInline) {
        SymbolLightClassForValueClass(
            ktAnalysisSession = this,
            ktModule = ktModule,
            classSymbol = classSymbol,
            manager = manager,
        )
    } else {
        SymbolLightClassForClassOrObject(
            ktAnalysisSession = this,
            ktModule = ktModule,
            classSymbol = classSymbol,
            manager = manager,
        )
    }
}

private fun lightClassForEnumEntry(ktEnumEntry: KtEnumEntry): KtLightClass? {
    if (ktEnumEntry.body == null) return null

    val symbolLightClass = ktEnumEntry.containingClass()?.toLightClass() as? SymbolLightClassForClassOrObject ?: return null
    val targetField = symbolLightClass.ownFields.firstOrNull {
        it is SymbolLightFieldForEnumEntry && it.kotlinOrigin == ktEnumEntry
    } ?: return null

    return (targetField as? SymbolLightFieldForEnumEntry)?.initializingClass as? KtLightClass
}

internal fun KaSession.createConstructors(
    lightClass: SymbolLightClassBase,
    declarations: Sequence<KaConstructorSymbol>,
    result: MutableList<PsiMethod>,
) {
    val constructors = declarations.toList()
    if (constructors.isEmpty()) {
        result.add(lightClass.defaultConstructor())
        return
    }

    for (constructor in constructors) {
        ProgressManager.checkCanceled()

        if (isHiddenOrSynthetic(constructor)) continue

        result.add(
            SymbolLightConstructor(
                ktAnalysisSession = this@createConstructors,
                constructorSymbol = constructor,
                containingClass = lightClass,
                methodIndex = METHOD_INDEX_BASE
            )
        )

        createJvmOverloadsIfNeeded(constructor, result) { methodIndex, argumentSkipMask ->
            SymbolLightConstructor(
                ktAnalysisSession = this@createConstructors,
                constructorSymbol = constructor,
                containingClass = lightClass,
                methodIndex = methodIndex,
                argumentsSkipMask = argumentSkipMask
            )
        }
    }
    val primaryConstructor = constructors.singleOrNull { it.isPrimary }
    if (primaryConstructor != null && shouldGenerateNoArgOverload(lightClass, primaryConstructor, constructors)) {
        result.add(
            lightClass.noArgConstructor(
                primaryConstructor.compilerVisibility.externalDisplayName,
                primaryConstructor.sourcePsiSafe(),
                METHOD_INDEX_FOR_NO_ARG_OVERLOAD_CTOR
            )
        )
    }
}

private fun KaSession.shouldGenerateNoArgOverload(
    lightClass: SymbolLightClassBase,
    primaryConstructor: KaConstructorSymbol,
    constructors: Iterable<KaConstructorSymbol>,
): Boolean {
    val classOrObject = lightClass.kotlinOrigin ?: return false
    return primaryConstructor.visibility != KaSymbolVisibility.PRIVATE &&
            !classOrObject.hasModifier(INNER_KEYWORD) && !lightClass.isEnum &&
            !classOrObject.hasModifier(SEALED_KEYWORD) &&
            primaryConstructor.valueParameters.isNotEmpty() &&
            primaryConstructor.valueParameters.all { it.hasDefaultValue } &&
            constructors.none { it.valueParameters.isEmpty() } &&
            !primaryConstructor.hasJvmOverloadsAnnotation()
}

private fun SymbolLightClassBase.defaultConstructor(): KtLightMethod {
    val classOrObject = kotlinOrigin
    val visibility = when {
        this is SymbolLightClassForClassLike<*> && (classKind().let { it.isObject || it == KaClassKind.ENUM_CLASS }) -> PsiModifier.PRIVATE
        classOrObject?.hasModifier(SEALED_KEYWORD) == true -> PsiModifier.PROTECTED
        this is SymbolLightClassForEnumEntry -> PsiModifier.PACKAGE_LOCAL
        else -> PsiModifier.PUBLIC
    }

    return noArgConstructor(visibility, classOrObject, METHOD_INDEX_FOR_DEFAULT_CTOR)
}

private fun SymbolLightClassBase.noArgConstructor(
    visibility: String,
    declaration: KtDeclaration?,
    methodIndex: Int,
): KtLightMethod = SymbolLightNoArgConstructor(
    declaration?.let {
        LightMemberOriginForDeclaration(
            originalElement = it,
            originKind = JvmDeclarationOriginKind.OTHER,
        )
    },
    this,
    visibility,
    methodIndex,
)

internal fun KaSession.createMethods(
    lightClass: SymbolLightClassBase,
    declarations: Sequence<KaCallableSymbol>,
    result: MutableList<PsiMethod>,
    isTopLevel: Boolean = false,
    suppressStatic: Boolean = false,
) {
    val (ctorProperties, regularMembers) = declarations.partition { it is KaPropertySymbol && it.isFromPrimaryConstructor }

    fun KaSession.handleDeclaration(declaration: KaCallableSymbol) {
        when (declaration) {
            is KaNamedFunctionSymbol -> createSimpleMethods(
                containingClass = lightClass,
                functionSymbol = declaration,
                result = result,
                lightMemberOrigin = null,
                methodIndex = METHOD_INDEX_BASE,
                isTopLevel = isTopLevel,
                suppressStatic = suppressStatic,
            )

            is KaPropertySymbol -> createPropertyAccessors(
                lightClass,
                result,
                declaration,
                isTopLevel = isTopLevel,
                suppressStatic = suppressStatic
            )

            is KaConstructorSymbol -> error("Constructors should be handled separately and not passed to this function")
            else -> {}
        }
    }

    // Regular members
    regularMembers.forEach {
        this@createMethods.handleDeclaration(it)
    }
    // Then, properties from the primary constructor parameters
    ctorProperties.forEach {
        this@createMethods.handleDeclaration(it)
    }
}

internal inline fun <T : KaFunctionSymbol> KaSession.createJvmOverloadsIfNeeded(
    declaration: T,
    result: MutableList<PsiMethod>,
    lightMethodCreator: (Int, BitSet) -> PsiMethod,
) {
    if (!declaration.hasJvmOverloadsAnnotation()) return
    var methodIndex = METHOD_INDEX_BASE
    val skipMask = BitSet(declaration.valueParameters.size)
    for (i in declaration.valueParameters.size - 1 downTo 0) {
        if (!declaration.valueParameters[i].hasDefaultValue) continue
        skipMask.set(i)
        result.add(
            lightMethodCreator.invoke(methodIndex++, skipMask.copy())
        )
    }
}

internal fun KaSession.createAndAddField(
    lightClass: SymbolLightClassBase,
    declaration: KaPropertySymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    isStatic: Boolean,
    result: MutableList<PsiField>,
) {
    val field = createField(lightClass, declaration, nameGenerator, isStatic) ?: return
    result += field
}

internal fun KaSession.createField(
    lightClass: SymbolLightClassBase,
    declaration: KaPropertySymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    isStatic: Boolean,
): SymbolLightFieldForProperty? {
    ProgressManager.checkCanceled()

    if (declaration.name.isSpecial) return null
    if (!hasBackingField(declaration)) return null

    val fieldName = nameGenerator.generateUniqueFieldName(declaration.name.asString())

    return SymbolLightFieldForProperty(
        ktAnalysisSession = this@createField,
        propertySymbol = declaration,
        fieldName = fieldName,
        containingClass = lightClass,
        lightMemberOrigin = null,
        isStatic = isStatic,
    )
}

private fun KaSession.hasBackingField(property: KaPropertySymbol): Boolean {
    if (property is KaSyntheticJavaPropertySymbol) return true
    requireIsInstance<KaKotlinPropertySymbol>(property)

    if (property.origin.cannotHasBackingField() || property.isStatic) return false
    if (property.isLateInit || property.isDelegatedProperty || property.isFromPrimaryConstructor) return true
    val hasBackingFieldByPsi: Boolean? = property.psi?.hasBackingField()
    if (hasBackingFieldByPsi == false) {
        return hasBackingFieldByPsi
    }

    if (property.isExpect ||
        property.modality == KaSymbolModality.ABSTRACT ||
        property.backingFieldSymbol?.hasJvmSyntheticAnnotation() == true
    ) return false

    return hasBackingFieldByPsi ?: property.hasBackingField
}

private fun KaSymbolOrigin.cannotHasBackingField(): Boolean =
    this == KaSymbolOrigin.SOURCE_MEMBER_GENERATED ||
            this == KaSymbolOrigin.DELEGATED ||
            this == KaSymbolOrigin.INTERSECTION_OVERRIDE ||
            this == KaSymbolOrigin.SUBSTITUTION_OVERRIDE

private fun PsiElement.hasBackingField(): Boolean {
    if (this is KtParameter) return true
    if (this !is KtProperty) return false

    return hasInitializer() || getter?.takeIf { it.hasBody() } == null || setter?.takeIf { it.hasBody() } == null && isVar
}

internal fun KaSession.createInheritanceList(
    lightClass: SymbolLightClassForClassLike<*>,
    forExtendsList: Boolean,
    superTypes: List<KaType>,
): PsiReferenceList {
    val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST

    val listBuilder = KotlinSuperTypeListBuilder(
        lightClass,
        kotlinOrigin = lightClass.kotlinOrigin?.getSuperTypeList(),
        manager = lightClass.manager,
        language = lightClass.language,
        role = role,
    )

    fun KaType.needToAddTypeIntoList(): Boolean {
        // Do not add redundant "extends java.lang.Object" anywhere
        if (this.isAnyType) return false
        // Interfaces have only extends lists
        if (lightClass.isInterface) return forExtendsList

        return when (this) {
            is KaClassType -> {
                // We don't have Enum among enums supertype in sources neither we do for decompiled class-files and light-classes
                if (lightClass.isEnum && this.classId == StandardClassIds.Enum) return false

                // NB: need to expand type alias, e.g., kotlin.Comparator<T> -> java.util.Comparator<T>
                val classKind = expandedSymbol?.classKind
                val isJvmInterface = classKind == KaClassKind.INTERFACE || classKind == KaClassKind.ANNOTATION_CLASS

                forExtendsList == !isJvmInterface
            }

            is KaClassErrorType -> {
                val superList = lightClass.kotlinOrigin?.getSuperTypeList() ?: return false
                val qualifierName = this.qualifiers.joinToString(".") { it.name.asString() }.takeIf { it.isNotEmpty() } ?: return false
                val isConstructorCall = superList.findEntry(qualifierName) is KtSuperTypeCallEntry

                forExtendsList == isConstructorCall
            }

            else -> false
        }
    }

    superTypes.asSequence()
        .filter { it.needToAddTypeIntoList() }
        .forEach { superType ->
            val mappedType = mapType(
                superType,
                lightClass,
                KaTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            ) ?: return@forEach
            listBuilder.addReference(mappedType)
            if (mappedType.canonicalText.startsWith("kotlin.collections.")) {
                val mappedToNoCollectionAsIs = mapType(superType, lightClass, KaTypeMappingMode.SUPER_TYPE)
                if (mappedToNoCollectionAsIs != null &&
                    mappedType.canonicalText != mappedToNoCollectionAsIs.canonicalText
                ) {
                    // Add java supertype
                    listBuilder.addReference(mappedToNoCollectionAsIs)
                    // Add marker interface
                    if (superType is KaClassType) {
                        listBuilder.addMarkerInterfaceIfNeeded(superType.classId)
                    }
                }
            }
        }

    return listBuilder
}

internal fun KaSession.createInnerClasses(
    declarationContainer: KaDeclarationContainerSymbol,
    manager: PsiManager,
    containingClass: SymbolLightClassBase,
    classOrObject: KtClassOrObject?,
): List<SymbolLightClassBase> {
    val result = SmartList<SymbolLightClassBase>()

    declarationContainer.staticDeclaredMemberScope.classifiers.filterIsInstance<KaNamedClassSymbol>().mapNotNullTo(result) {
        val classOrObjectDeclaration = it.sourcePsiSafe<KtClassOrObject>()
        if (classOrObjectDeclaration != null) {
            classOrObjectDeclaration.toLightClass() as? SymbolLightClassBase
        } else {
            createLightClassNoCache(it, ktModule = containingClass.ktModule, manager)
        }
    }

    val jvmDefaultMode = classOrObject
        ?.let { getModule(it) as? KaSourceModule }
        ?.languageVersionSettings
        ?.getFlag(JvmAnalysisFlags.jvmDefaultMode)
        ?: JvmDefaultMode.DISABLE

    if (containingClass is SymbolLightClassForInterface &&
        classOrObject?.hasInterfaceDefaultImpls == true &&
        jvmDefaultMode != JvmDefaultMode.ALL
    ) {
        result.add(SymbolLightClassForInterfaceDefaultImpls(containingClass))
    }

    if (containingClass is SymbolLightClassForAnnotationClass &&
        declarationContainer is KaNamedClassSymbol &&
        StandardClassIds.Annotations.Repeatable in declarationContainer.annotations &&
        JvmStandardClassIds.Annotations.Java.Repeatable !in declarationContainer.annotations
    ) {
        result.add(SymbolLightClassForRepeatableAnnotationContainer(containingClass))
    }

    return result
}

internal fun KaSession.checkIsInheritor(classOrObject: KtClassOrObject, superClassOrigin: KtClassOrObject, checkDeep: Boolean): Boolean {
    if (classOrObject == superClassOrigin) return false
    if (superClassOrigin is KtEnumEntry) {
        return false // enum entry cannot have inheritors
    }
    if (!superClassOrigin.canBeAnalysed()) {
        return false
    }

    val superClassSymbol = superClassOrigin.classSymbol ?: return false

    when (classOrObject) {
        is KtEnumEntry -> {
            val enumEntrySymbol = classOrObject.symbol
            val classId = enumEntrySymbol.callableId?.classId ?: return false
            val enumClassSymbol = findClass(classId) ?: return false
            if (enumClassSymbol == superClassSymbol) return true
            return if (checkDeep) {
                enumClassSymbol.isSubClassOf(superClassSymbol)
            } else {
                false
            }
        }

        else -> {
            val subClassSymbol = classOrObject.classSymbol

            if (subClassSymbol == null || subClassSymbol == superClassSymbol) return false

            return if (checkDeep) {
                subClassSymbol.isSubClassOf(superClassSymbol)
            } else {
                subClassSymbol.isDirectSubClassOf(superClassSymbol)
            }
        }
    }
}

internal val KaDeclarationSymbol.hasReifiedParameters: Boolean
    get() = typeParameters.any { it.isReified }

internal fun KaSession.addPropertyBackingFields(
    lightClass: SymbolLightClassBase,
    result: MutableList<PsiField>,
    containerSymbol: KaDeclarationContainerSymbol,
    nameGenerator: SymbolLightField.FieldNameGenerator,
    forceIsStaticTo: Boolean? = null,
) {
    val propertySymbols = containerSymbol.combinedDeclaredMemberScope.callables
        .filterIsInstance<KaPropertySymbol>()
        .applyIf(containerSymbol is KaClassSymbol && containerSymbol.classKind == KaClassKind.COMPANION_OBJECT) {
            // All fields for companion object of classes are generated to the containing class
            // For interfaces, only @JvmField-annotated properties are generated to the containing class
            // Probably, the same should work for const vals but it doesn't at the moment (see KT-28294)
            filter { lightClass.containingClass?.isInterface == true && !it.isJvmField }
        }

    val (ctorProperties, memberProperties) = propertySymbols.partition { it.isFromPrimaryConstructor }
    val isStatic = forceIsStaticTo ?: (containerSymbol is KaClassSymbol && containerSymbol.classKind.isObject)
    fun addPropertyBackingField(propertySymbol: KaPropertySymbol) {
        createAndAddField(
            lightClass = lightClass,
            declaration = propertySymbol,
            nameGenerator = nameGenerator,
            isStatic = isStatic,
            result = result
        )
    }

    // First, properties from parameters
    ctorProperties.forEach(::addPropertyBackingField)
    // Then, regular member properties
    memberProperties.forEach(::addPropertyBackingField)
}

/**
 * @param suppressJvmNameCheck **true** if [hasJvmNameAnnotation] should be omitted.
 * E.g., if [JvmName] is checked manually later
 */
internal fun KaSession.hasTypeForValueClassInSignature(
    callableSymbol: KaCallableSymbol,
    ignoreReturnType: Boolean = false,
    suppressJvmNameCheck: Boolean = false,
): Boolean {
    // Declarations with JvmName can be accessible from Java
    when {
        suppressJvmNameCheck -> {}
        callableSymbol.hasJvmNameAnnotation() -> return false
        callableSymbol !is KaKotlinPropertySymbol -> {}
        callableSymbol.getter?.hasJvmNameAnnotation() == true || callableSymbol.setter?.hasJvmNameAnnotation() == true -> return false
    }

    if (!ignoreReturnType) {
        val psiDeclaration = callableSymbol.psi as? KtCallableDeclaration
        // Only explicitly declared types can be checked to avoid contract violations
        if (psiDeclaration?.typeReference != null && typeForValueClass(callableSymbol.returnType)) return true
    }

    if (callableSymbol.receiverType?.let { typeForValueClass(it) } == true) return true
    if (callableSymbol is KaFunctionSymbol) {
        return callableSymbol.valueParameters.any { typeForValueClass(it.returnType) }
    }

    return false
}

internal fun KaSession.typeForValueClass(type: KaType): Boolean {
    val symbol = type.expandedSymbol as? KaNamedClassSymbol ?: return false
    return symbol.isInline
}
