/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.generators.isFakeOverride
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.backend.utils.computeValueClassRepresentation
import org.jetbrains.kotlin.fir.backend.utils.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.utils.getIrSymbolsForSealedSubclasses
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.hasEnumEntries
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.scopes.staticScopeForBackend
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.deserializedIr
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name

class Fir2IrLazyClass(
    private val c: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirRegularClass,
    override val symbol: IrClassSymbol,
    parent: IrDeclarationParent,
) : IrClass(), AbstractFir2IrLazyDeclaration<FirRegularClass>, Fir2IrTypeParametersContainer,
    IrMaybeDeserializedClass, Fir2IrComponents by c {
    init {
        this.parent = parent
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
        this.deserializedIr = lazy {
            assert(parent is IrPackageFragment)
            extensions.deserializeToplevelClass(this, this)
        }
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
    override lateinit var typeParameters: List<IrTypeParameter>

    override val source: SourceElement
        get() = fir.sourceElement ?: SourceElement.NO_SOURCE

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var name: Name
        get() = fir.name
        set(_) = mutationNotSupported()

    override var visibility: DescriptorVisibility = c.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var modality: Modality
        get() = if (fir.classKind.isAnnotationClass) Modality.OPEN else fir.symbol.resolvedStatus.modality
        set(_) = mutationNotSupported()

    override var attributeOwnerId: IrAttributeContainer
        get() = this
        set(_) = mutationNotSupported()

    override var originalBeforeInline: IrAttributeContainer?
        get() = null
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override var kind: ClassKind
        get() = fir.classKind
        set(_) = mutationNotSupported()

    override var isCompanion: Boolean
        get() = fir.isCompanion
        set(_) = mutationNotSupported()

    override var isInner: Boolean
        get() = fir.isInner
        set(_) = mutationNotSupported()

    override var isData: Boolean
        get() = fir.isData
        set(_) = mutationNotSupported()

    override var isExternal: Boolean
        get() = fir.isExternal
        set(_) = mutationNotSupported()

    override var isValue: Boolean
        get() = fir.isInline
        set(_) = mutationNotSupported()

    override var isExpect: Boolean
        get() = fir.isExpect
        set(_) = mutationNotSupported()

    override var isFun: Boolean
        get() = fir.isFun
        set(_) = mutationNotSupported()

    override var hasEnumEntries: Boolean
        get() = fir.hasEnumEntries
        set(_) = mutationNotSupported()

    override var superTypes: List<IrType> by lazyVar(lock) {
        fir.superTypeRefs.map { it.toIrType(typeConverter) }
    }

    override var sealedSubclasses: List<IrClassSymbol> by lazyVar(lock) {
        if (fir.isSealed) {
            fir.getIrSymbolsForSealedSubclasses(c)
        } else {
            emptyList()
        }
    }

    override var thisReceiver: IrValueParameter? by lazyVar(lock) {
        val typeArguments = fir.typeParameters.map {
            IrSimpleTypeImpl(
                classifierStorage.getCachedIrTypeParameter(it.symbol.fir)!!.symbol,
                hasQuestionMark = false, arguments = emptyList(), annotations = emptyList()
            )
        }
        val receiver = declareThisReceiverParameter(
            c,
            thisType = IrSimpleTypeImpl(symbol, hasQuestionMark = false, arguments = typeArguments, annotations = emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        receiver
    }

    override var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>?
        get() = computeValueClassRepresentation(fir)
        set(_) = mutationNotSupported()

    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> by lazyVar(lock) {
        val result = mutableListOf<IrDeclaration>()
        // NB: it's necessary to take all callables from scope,
        // e.g. to avoid accessing un-enhanced Java declarations with FirJavaTypeRef etc. inside
        val scope = fir.unsubstitutedScope(c)
        val lookupTag = fir.symbol.toLookupTag()
        scope.processDeclaredConstructors {
            val constructor = it.fir
            if (shouldBuildStub(constructor)) {
                // Lazy declarations are created together with their symbol, so it's safe to take the owner here
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                result += declarationStorage.getIrConstructorSymbol(constructor.symbol).owner
            }
        }

        for (name in scope.getClassifierNames()) {
            scope.processClassifiersByName(name) {
                val declaration = it.fir as? FirRegularClass ?: return@processClassifiersByName
                if (declaration.classId.outerClassId == fir.classId && shouldBuildStub(declaration)) {
                    result += classifierStorage.getIrClassSymbol(declaration.symbol).owner
                }
            }
        }

        if (fir.classKind == ClassKind.ENUM_CLASS) {
            for (declaration in fir.declarations) {
                if (declaration is FirEnumEntry && shouldBuildStub(declaration)) {
                    result += classifierStorage.getIrEnumEntrySymbol(declaration).owner
                }
            }
        }

        fun addDeclarationsFromScope(scope: FirContainingNamesAwareScope?) {
            if (scope == null) return
            for (name in scope.getCallableNames()) {
                scope.processFunctionsByName(name) { symbol ->
                    when {
                        !shouldBuildStub(symbol.fir) -> {}
                        else -> {
                            // Lazy declarations are created together with their symbol, so it's safe to take the owner here
                            @OptIn(UnsafeDuringIrConstructionAPI::class)
                            result += declarationStorage.getIrFunctionSymbol(symbol, lookupTag).owner
                        }
                    }
                }
                scope.processPropertiesByName(name) { symbol ->
                    when {
                        !shouldBuildStub(symbol.fir) -> {}
                        symbol is FirFieldSymbol -> {
                            if (shouldBuildIrField(symbol)) {
                                // Lazy declarations are created together with their symbol, so it's safe to take the owner here
                                @OptIn(UnsafeDuringIrConstructionAPI::class)
                                result += declarationStorage.getIrSymbolForField(
                                    symbol,
                                    fakeOverrideOwnerLookupTag = lookupTag
                                ).owner as IrProperty
                            }
                        }
                        symbol is FirPropertySymbol -> {
                            // Lazy declarations are created together with their symbol, so it's safe to take the owner here
                            @OptIn(UnsafeDuringIrConstructionAPI::class)
                            result += declarationStorage.getIrPropertySymbol(symbol, lookupTag).owner as IrProperty
                        }
                        else -> {}
                    }
                }
            }
        }

        addDeclarationsFromScope(scope)
        addDeclarationsFromScope(fir.staticScopeForBackend(session, scopeSession))

        with(classifierStorage) {
            result.addAll(getFieldsWithContextReceiversForClass(this@Fir2IrLazyClass, fir))
        }

        result
    }

    private fun shouldBuildStub(fir: FirDeclaration): Boolean {
        if (fir is FirCallableDeclaration) {
            if (fir.originalOrSelf().origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk) {
                return false
            }
            if (fir.isHiddenToOvercomeSignatureClash == true && fir.isFinal) {
                return false
            }
        }
        if (fir !is FirMemberDeclaration) return true
        return when {
            fir is FirConstructor -> isObject || isEnumClass || !Visibilities.isPrivate(fir.visibility) // This special case seams to be not needed anymore - KT-65172
            fir is FirCallableDeclaration && fir.isFakeOverride(this.fir) -> session.visibilityChecker.isVisibleForOverriding(
                this.fir.moduleData,
                this.fir.symbol,
                fir
            )
            else -> !Visibilities.isPrivate(fir.visibility)
        }
    }

    private fun shouldBuildIrField(fieldSymbol: FirFieldSymbol): Boolean {
        if (!fieldSymbol.isStatic) return true
        // we need to create IR for static fields only if they are not fake-overrides
        return fir.isJava && !fieldSymbol.fir.isFakeOverride(fir)
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val moduleName: String?
        get() = fir.moduleName

    override val isNewPlaceForBodyGeneration: Boolean
        get() = fir.isNewPlaceForBodyGeneration == true
}
