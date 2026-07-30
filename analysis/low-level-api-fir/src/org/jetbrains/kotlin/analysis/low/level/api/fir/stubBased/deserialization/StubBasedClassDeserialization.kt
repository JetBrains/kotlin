/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusWithLazyEffectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.deserialization.addCloneForArrayIfNeeded
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.deserialization.applyKDoc
import org.jetbrains.kotlin.fir.deserialization.deserializationExtension
import org.jetbrains.kotlin.fir.deserialization.toLazyEffectiveVisibility
import org.jetbrains.kotlin.fir.resolve.transformers.setLazyPublishedVisibility
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.ConeRigidType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KlibConstants.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.readKonanLibraryVersioning
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFullValueClassRepresentation
import org.jetbrains.kotlin.psi.stubs.impl.KotlinInlineClassRepresentation
import org.jetbrains.kotlin.psi.stubs.impl.KotlinRigidTypeBean
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.Properties

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
    get() = when {
        hasModifier(KtTokens.SEALED_KEYWORD) -> Modality.SEALED
        hasModifier(KtTokens.ABSTRACT_KEYWORD) || this is KtClass && isInterface() -> Modality.ABSTRACT
        hasModifier(KtTokens.OPEN_KEYWORD) -> Modality.OPEN
        else -> Modality.FINAL
    }

/**
 * Gets or calculates stub for [this] element and casts it to [S].
 *
 * [S] has to be a real stub implementation class. For instance, for [KtNamedFunction] it has to be [org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl].
 *
 * @return compiled stub
 */
internal inline val <T, reified S> T.compiledStub: S where T : StubBasedPsiElementBase<in S>, T : KtElement, S : StubElement<*>
    get() = (this.greenStub ?: calculateStub()) as S

private fun <S, T> T.calculateStub(): Stub where T : StubBasedPsiElementBase<in S>, T : KtElement, S : StubElement<*> {
    val ktFile = containingKtFile
    requireWithAttachment(ktFile.isCompiled, { "Expected compiled file" }) {
        withPsiEntry("ktFile", ktFile)
    }

    // `let` is used to hold the stub tree reference on the stack
    return ktFile.calcStubTree().let {
        val stub = greenStub
        requireWithAttachment(stub != null, { "Stub should be not null" }) {
            withPsiEntry("file", containingFile)
            withPsiEntry("element", this@calculateStub)
        }

        stub
    }
}

internal fun deserializeClassToSymbol(
    classId: ClassId,
    classOrObject: KtClassOrObject,
    symbol: FirRegularClassSymbol,
    session: FirSession,
    moduleData: FirModuleData,
    defaultAnnotationDeserializer: StubBasedAnnotationDeserializer?,
    scopeProvider: FirScopeProvider,
    parentContext: StubBasedFirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    deserializeNestedClassLikeDeclaration: (ClassId, KtClassLikeDeclaration, StubBasedFirDeserializationContext) -> FirClassLikeSymbol<*>?,
    initialOrigin: FirDeclarationOrigin,
) {
    val kind = when (classOrObject) {
        is KtObjectDeclaration -> ClassKind.OBJECT
        is KtClass -> when {
            classOrObject.isInterface() -> ClassKind.INTERFACE
            classOrObject.isEnum() -> ClassKind.ENUM_CLASS
            classOrObject.isAnnotation() -> ClassKind.ANNOTATION_CLASS
            else -> ClassKind.CLASS
        }
        else -> errorWithAttachment("Unexpected class or object: ${classOrObject::class}") {
            withPsiEntry("class", classOrObject)
        }
    }
    val modality = classOrObject.modality
    val visibility = classOrObject.visibility
    val status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
        visibility,
        modality,
        visibility.toLazyEffectiveVisibility(parentContext?.outerClassSymbol, session, forClass = true)
    ).apply {
        isExpect = classOrObject.hasModifier(KtTokens.EXPECT_KEYWORD)
        isActual = false
        isInner = classOrObject.hasModifier(KtTokens.INNER_KEYWORD)
        isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
        isData = classOrObject.hasModifier(KtTokens.DATA_KEYWORD)
        isInline = classOrObject.hasModifier(KtTokens.INLINE_KEYWORD)
        isValue = classOrObject.hasModifier(KtTokens.VALUE_KEYWORD)
        isFun = classOrObject.hasModifier(KtTokens.FUN_KEYWORD)
        isExternal = classOrObject.hasModifier(KtTokens.EXTERNAL_KEYWORD)
    }
    val annotationDeserializer = defaultAnnotationDeserializer ?: StubBasedAnnotationDeserializer(session)
    val context =
        parentContext?.childContext(
            classOrObject,
            classId.relativeClassName,
            containerSource,
            symbol,
            annotationDeserializer,
            status.isInner
        ) ?: StubBasedFirDeserializationContext.createForClass(
            classId,
            classOrObject,
            moduleData,
            annotationDeserializer,
            containerSource,
            symbol,
            initialOrigin
        )

    val classStub: KotlinClassStubImpl? = (classOrObject as? KtClass)?.compiledStub

    buildRegularClass {
        val sourceElement = KtRealPsiSourceElement(classOrObject)

        source = sourceElement
        this.moduleData = moduleData
        this.origin = initialOrigin
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
        val memberDeserializer = context.memberDeserializer

        val superTypeList = classOrObject.getSuperTypeList()
        if (superTypeList != null) {
            superTypeRefs.addAll(superTypeList.entries.map { superTypeReference ->
                typeDeserializer.typeRef(
                    superTypeReference.typeReference
                        ?: errorWithAttachment("Super entry doesn't have type reference") {
                            withPsiEntry("superTypeReference", superTypeReference)
                        }
                )
            })
        } else if (StandardClassIds.Any != classId && StandardClassIds.Nothing != classId) {
            superTypeRefs.add(session.builtinTypes.anyType)
        }

        classOrObject.primaryConstructor?.let { constructor ->
            val firConstructor = memberDeserializer.loadConstructor(constructor, classOrObject, this)
            addDeclaration(firConstructor)

            // A property folded into its parameter has no member declaration of its own to be built from
            for ([index, parameter] in constructor.valueParameters.withIndex()) {
                if (!parameter.hasValOrVar()) continue

                val property = memberDeserializer.loadPropertyFromParameter(parameter, symbol)
                firConstructor.valueParameters[index].correspondingProperty = property
                addDeclaration(property)
            }
        }

        @OptIn(KtExperimentalApi::class)
        classOrObject.body
            ?.declarationsAndCompanionBlocks
            ?.asSequence()
            ?.flatMap { if (it is KtCompanionBlock) it.declarations else listOf(it) }
            ?.forEach { declaration ->
                when (declaration) {
                    is KtConstructor<*> -> addDeclaration(memberDeserializer.loadConstructor(declaration, classOrObject, this))
                    is KtNamedFunction -> addDeclaration(memberDeserializer.loadFunction(declaration, symbol, session))
                    is KtProperty -> addDeclaration(
                        memberDeserializer.loadProperty(
                            property = declaration,
                            classSymbol = symbol,
                            isFromAnnotation = kind == ClassKind.ANNOTATION_CLASS,
                        )
                    )
                    is KtEnumEntry -> addDeclaration(memberDeserializer.loadEnumEntry(declaration, symbol, classId))
                    is KtClassLikeDeclaration -> {
                        val name = declaration.name
                            ?: errorWithAttachment("${if (declaration is KtClassOrObject) "Class" else "Typealias"} doesn't have name") {
                                withPsiEntry(if (declaration is KtClassOrObject) "Class" else "Typealias", declaration)
                            }

                        val nestedClassId = classId.createNestedClassId(Name.identifier(name))
                        // Add declaration to the context to avoid redundant provider access to the class/typealias map
                        deserializeNestedClassLikeDeclaration(
                            nestedClassId,
                            declaration,
                            context.withClassLikeDeclaration(declaration),
                        )?.fir?.let(this::addDeclaration)
                    }
                }
            }

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = initialOrigin
            )
            generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName, origin = initialOrigin)
            generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName, origin = initialOrigin)
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver, session)
        addCloneForEnumIfNeeded(classId, classOrObject, sourceElement, context.dispatchReceiver)

        session.deserializationExtension?.run {
            configureDeserializedClass(classId)
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

        contextParameters.addAll(memberDeserializer.createContextReceiversForClass(classOrObject, symbol))

        if (classStub != null) {
            applyKDoc(classStub.kdocText)
        }
    }.apply {
        if (classStub != null) {
            if (isInlineOrValue) {
                valueClassRepresentation = classStub.deserializeValueClassRepresentation(this, context.typeDeserializer)
            }

            val clsStubCompiledToJvmDefaultImplementation = classStub.isClsStubCompiledToJvmDefaultImplementation
            if (clsStubCompiledToJvmDefaultImplementation) {
                symbol.fir.isNewPlaceForBodyGeneration = true
            }
        }

        replaceAnnotations(context.annotationDeserializer.loadAnnotations(classOrObject))

        sourceElement = containerSource

        replaceDeprecationsProvider(getDeprecationsProvider(session))

        setLazyPublishedVisibility(
            hasPublishedApi = classOrObject.annotationEntries.any { StubBasedAnnotationDeserializer.getAnnotationClassId(it) == StandardClassIds.Annotations.PublishedApi },
            parentProperty = null,
            session
        )
    }
}

@OptIn(KtImplementationDetail::class)
private fun KotlinClassStubImpl.deserializeValueClassRepresentation(
    klass: FirRegularClass,
    typeDeserializer: StubBasedFirTypeDeserializer,
): ValueClassRepresentation<ConeRigidType>? = when (val representation = valueClassRepresentation) {
    null -> null

    is KotlinInlineClassRepresentation -> InlineClassRepresentation(
        underlyingPropertyName = representation.underlyingPropertyName,
        underlyingType = typeDeserializer.underlyingType(representation.underlyingType, klass),
    )

    is KotlinFullValueClassRepresentation -> FullValueClassRepresentation(
        underlyingPropertyNamesToTypes = representation.underlyingPropertyNamesToTypes
            ?.map { [name, type] -> name to typeDeserializer.underlyingType(type, klass) }
    )
}

private fun StubBasedFirTypeDeserializer.underlyingType(bean: KotlinRigidTypeBean, klass: FirRegularClass): ConeRigidType {
    val type = type(bean) ?: errorWithAttachment("Cannot determine the underlying type of a value class") {
        withEntry("underlyingType", bean.toString())
        withFirEntry("class", klass)
    }

    requireWithAttachment(type is ConeRigidType, { "Underlying type must be a rigid type" }) {
        withConeTypeEntry("type", type)
        withFirEntry("class", klass)
    }

    return type
}

private fun FirRegularClassBuilder.addCloneForEnumIfNeeded(
    classId: ClassId,
    classOrObject: KtClassOrObject,
    classSourceElement: KtSourceElement,
    dispatchReceiver: ConeClassLikeType?,
) {
    if (classId != StandardClassIds.Enum) {
        return
    }

    val hasCloneFunction = classOrObject.declarations
        .any { it is KtNamedFunction && it.name == "clone" && it.valueParameters.isEmpty() }

    if (hasCloneFunction) {
        return
    }

    if (!requiresSyntheticEnumClone(moduleData)) {
        return
    }

    val anyLookupId = StandardClassIds.Any.toLookupTag()
    val cloneCallableId = StandardClassIds.Callables.clone

    val sourceElement = classSourceElement.fakeElement(KtFakeSourceElementKind.EnumGeneratedDeclaration.CloneFunction)

    declarations += buildNamedFunction {
        moduleData = this@addCloneForEnumIfNeeded.moduleData
        origin = this@addCloneForEnumIfNeeded.origin
        source = sourceElement

        resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

        returnTypeRef = buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(anyLookupId, typeArguments = emptyArray(), isMarkedNullable = false)
        }

        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Protected,
            Modality.FINAL,
            EffectiveVisibility.Protected(anyLookupId)
        )
        isLocal = false

        name = cloneCallableId.callableName
        symbol = FirNamedFunctionSymbol(cloneCallableId)
        dispatchReceiverType = dispatchReceiver!!
    }
}

/**
 * Whether the `clone()` member has to be synthesized for the `kotlin.Enum` coming from [moduleData].
 *
 * Standard libraries built before Kotlin 2.1 – before the built-ins were rewritten as `expect`/`actual` (KT-65526) – do not
 * declare `clone()` on `kotlin.Enum` for non-JVM platforms, while their common `kotlin.Enum` still exposes it. Because of that
 * asymmetry, every `expect`/`actual` enum reports a false-positive `NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS` for the missing
 * `clone()`. To stay consistent with such legacy standard libraries, we re-introduce the synthetic `clone()` for them
 * (KT-65417).
 *
 * Modern (2.1+) standard libraries declare no `clone()` on `kotlin.Enum` on either side, so the member must not be synthesized.
 */
private fun requiresSyntheticEnumClone(moduleData: FirModuleData): Boolean {
    val libraryModule = (moduleData as? LLFirModuleData)?.ktModule as? KaLibraryModule ?: return false
    val compilerVersion = libraryModule.binaryVirtualFiles.firstNotNullOfOrNull { it.klibCompilerVersion() } ?: return false
    return isPre21StdlibVersion(compilerVersion)
}

/**
 * Reads the `compiler_version` from the klib manifest reachable from this klib root [VirtualFile], or `null` if it cannot be determined.
 */
private fun VirtualFile.klibCompilerVersion(): String? {
    val manifestFile = findFileByRelativePath("$KLIB_DEFAULT_COMPONENT_NAME/$KLIB_MANIFEST_FILE_NAME") ?: return null
    val versioning = try {
        manifestFile.inputStream.use { Properties().apply { load(it) } }.readKonanLibraryVersioning()
    } catch (e: Exception) {
        rethrowIntellijPlatformExceptionIfNeeded(e)
        return null
    }
    return versioning.compilerVersion
}

/**
 * Whether [compilerVersion] (a klib `compiler_version` such as `2.0.0-RC2-200`) denotes a Kotlin release older than 2.1, i.e.
 * one that predates the `expect`/`actual` built-ins rewrite.
 */
private fun isPre21StdlibVersion(compilerVersion: String): Boolean {
    val [major, minor] = COMPILER_VERSION_REGEX.find(compilerVersion)?.destructured ?: return false
    val majorNumber = major.toIntOrNull() ?: return false
    val minorNumber = minor.toIntOrNull() ?: return false
    return majorNumber < 2 || majorNumber == 2 && minorNumber < 1
}

private val COMPILER_VERSION_REGEX = Regex("""^(\d+)\.(\d+)""")
