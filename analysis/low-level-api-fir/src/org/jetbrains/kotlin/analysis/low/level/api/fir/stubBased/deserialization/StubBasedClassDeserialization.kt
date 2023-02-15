/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal val KtModifierListOwner.visibility: Visibility
    get() = with(modifierList) {
        when {
            this == null -> Visibilities.Public
            hasModifier(KtTokens.PRIVATE_KEYWORD) -> Visibilities.Private
            hasModifier(KtTokens.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(KtTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
            else -> if (hasModifier(KtTokens.INTERNAL_KEYWORD)) Visibilities.Internal else Visibilities.Public
        }
    }

internal val KtDeclaration.modality: Modality
    get() {
        return when {
            hasModifier(KtTokens.SEALED_KEYWORD) -> Modality.SEALED
            hasModifier(KtTokens.ABSTRACT_KEYWORD) || this is KtClass && isInterface() -> Modality.ABSTRACT
            hasModifier(KtTokens.OPEN_KEYWORD) -> Modality.OPEN
            else -> Modality.FINAL
        }
    }

fun deserializeClassToSymbol(
    classId: ClassId,
    classOrObject: KtClassOrObject,
    symbol: FirRegularClassSymbol,
    session: FirSession,
    moduleData: FirModuleData,
    defaultAnnotationDeserializer: StubBasedAbstractAnnotationDeserializer?,
    scopeProvider: FirScopeProvider,
    parentContext: StubBasedFirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    deserializeNestedClass: (ClassId, StubBasedFirDeserializationContext) -> FirRegularClassSymbol?
) {
    val kind = when (classOrObject) {
        is KtObjectDeclaration -> ClassKind.OBJECT
        is KtClass -> when {
            classOrObject.isInterface() -> ClassKind.INTERFACE
            classOrObject.isEnum() -> ClassKind.ENUM_CLASS
            classOrObject.isAnnotation() -> ClassKind.ANNOTATION_CLASS
            else -> ClassKind.CLASS
        }
        else -> throw AssertionError("Unexpected class or object: ${classOrObject.text}")
    }
    val modality = classOrObject.modality
    val visibility = classOrObject.visibility
    val status = FirResolvedDeclarationStatusImpl(
        visibility,
        modality,
        visibility.toEffectiveVisibility(parentContext?.outerClassSymbol, forClass = true)
    ).apply {
        isExpect = classOrObject.hasModifier(KtTokens.EXPECT_KEYWORD)
        isActual = false
        classOrObject is KtObjectDeclaration && classOrObject.isCompanion()
        isInner = classOrObject.hasModifier(KtTokens.INNER_KEYWORD)
        isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
        isData = classOrObject.hasModifier(KtTokens.DATA_KEYWORD)
        isInline = classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) || classOrObject.hasModifier(KtTokens.VALUE_KEYWORD)
        isFun = classOrObject.hasModifier(KtTokens.FUN_KEYWORD)
        isExternal = classOrObject.hasModifier(KtTokens.EXTERNAL_KEYWORD)
    }
    val annotationDeserializer = defaultAnnotationDeserializer ?: StubBasedFirBuiltinAnnotationDeserializer(session)
    val jvmBinaryClass = (containerSource as? KotlinJvmBinarySourceElement)?.binaryClass
    val constDeserializer = if (jvmBinaryClass != null) {
        StubBasedFirJvmConstDeserializer(session, jvmBinaryClass)
    } else {
        FirConstDeserializer(session)
    }
    val context =
        parentContext?.childContext(
            classOrObject,
            classId.relativeClassName,
            containerSource,
            symbol,
            annotationDeserializer,
            if (status.isCompanion) {
                parentContext.constDeserializer
            } else {
                ((containerSource as? KotlinJvmBinarySourceElement)?.binaryClass)?.let {
                    StubBasedFirJvmConstDeserializer(
                        session,
                        it
                    )
                } ?: parentContext.constDeserializer
            },
            status.isInner
        ) ?: StubBasedFirDeserializationContext.createForClass(
            classId,
            classOrObject,
            moduleData,
            annotationDeserializer,
            constDeserializer,
            containerSource,
            symbol
        )
    if (status.isCompanion) {
        parentContext?.let {
            context.annotationDeserializer.inheritAnnotationInfo(it.annotationDeserializer)
        }
    }
    buildRegularClass {
        this.moduleData = moduleData
        this.origin = FirDeclarationOrigin.Library
        name = classId.shortClassName
        this.status = status
        classKind = kind
        this.scopeProvider = scopeProvider
        this.symbol = symbol

        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

        typeParameters += context.typeDeserializer.ownTypeParameters.map { it.fir }
        if (status.isInner)
            typeParameters += parentContext?.allTypeParameters?.map { buildOuterClassTypeParameterRef { this.symbol = it } }.orEmpty()

        val typeDeserializer = context.typeDeserializer
        val classDeserializer = context.memberDeserializer

        val superTypeList = classOrObject.getSuperTypeList()
        if (superTypeList != null) {
            superTypeRefs.addAll(superTypeList.entries.map {
                typeDeserializer.typeRef(
                    it.typeReference ?: error("Super entry doesn't have type reference $it")
                )
            })
        } else if (StandardClassIds.Any != classId) {
            superTypeRefs.add(session.builtinTypes.anyType)
        }

        classOrObject.primaryConstructor?.let {
            addDeclaration(classDeserializer.loadConstructor(it, classOrObject, this))
        }
        classOrObject.body?.declarations?.forEach { declaration ->
            when (declaration) {
                is KtConstructor<*> -> addDeclaration(classDeserializer.loadConstructor(declaration, classOrObject, this))
                is KtNamedFunction -> addDeclaration(classDeserializer.loadFunction(declaration, classOrObject, symbol, session))
                is KtProperty -> addDeclaration(classDeserializer.loadProperty(declaration, classOrObject, symbol))
                is KtEnumEntry -> {
                    val enumEntryName = declaration.name ?: error("Enum entry doesn't provide name $declaration")

                    val enumType = ConeClassLikeTypeImpl(symbol.toLookupTag(), emptyArray(), false)
                    val property = buildEnumEntry {
                        this.moduleData = moduleData
                        this.origin = FirDeclarationOrigin.Library
                        returnTypeRef = buildResolvedTypeRef { type = enumType }
                        name = Name.identifier(enumEntryName)
                        this.symbol = FirEnumEntrySymbol(CallableId(classId, name))
                        this.status = FirResolvedDeclarationStatusImpl(
                            Visibilities.Public,
                            Modality.FINAL,
                            EffectiveVisibility.Public
                        ).apply {
                            isStatic = true
                        }
                        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    }.apply {
                        containingClassForStaticMemberAttr = context.dispatchReceiver!!.lookupTag
                    }

                    addDeclaration(property)
                }
                is KtClassOrObject -> {
                    val nestedClassId =
                        classId.createNestedClassId(Name.identifier(declaration.name ?: error("Class doesn't have name $declaration")))
                    deserializeNestedClass(nestedClassId, context)?.fir?.let { addDeclaration(it) }
                }
            }
        }

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(
                moduleData,
                classId.packageFqName,
                classId.relativeClassName
            )
            generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName)
            generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName)
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver)
        session.deserializedClassConfigurator?.run {
            configure(classId)
        }

        declarations.sortWith(object : Comparator<FirDeclaration> {
            override fun compare(a: FirDeclaration, b: FirDeclaration): Int {
                // Reorder members based on their type and name only.
                // See FE 1.0's [DeserializedMemberScope#addMembers].
                if (a is FirMemberDeclaration && b is FirMemberDeclaration) {
                    return FirMemberDeclarationComparator.TypeAndNameComparator.compare(a, b)
                }
                return 0
            }
        })
        companionObjectSymbol = (declarations.firstOrNull { it is FirRegularClass && it.isCompanion } as FirRegularClass?)?.symbol

        contextReceivers.addAll(classDeserializer.createContextReceiversForClass(classOrObject))
    }.apply {
        //todo sealed inheritors
        //if (modality == Modality.SEALED) {
//            val inheritors = classOrObject.sealedSubclassFqNameList.map { nameIndex ->
//                ClassId.fromString(nameResolver.getQualifiedClassName(nameIndex))
//            }
//            setSealedClassInheritors(inheritors)
        //}

        valueClassRepresentation = computeValueClassRepresentation(this, session)

        replaceAnnotations(
            context.annotationDeserializer.loadClassAnnotations(classOrObject)
        )


        sourceElement = containerSource

        replaceDeprecationsProvider(getDeprecationsProvider(session))

        session.deserializedClassConfigurator?.run {
            configure(classId)
        }
    }
}