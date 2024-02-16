/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.generators.FirBasedFakeOverrideGenerator
import org.jetbrains.kotlin.fir.backend.generators.isFakeOverride
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
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
import org.jetbrains.kotlin.ir.util.DeserializableClass
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name

@OptIn(FirBasedFakeOverrideGenerator::class) // only for lazy
class Fir2IrLazyClass(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirRegularClass,
    override val symbol: IrClassSymbol,
    override var parent: IrDeclarationParent,
) : IrClass(), AbstractFir2IrLazyDeclaration<FirRegularClass>, Fir2IrTypeParametersContainer,
    IrMaybeDeserializedClass, DeserializableClass, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir, symbol)
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

    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var modality: Modality
        get() = if (fir.classKind.isAnnotationClass) Modality.OPEN else fir.modality!!
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
            fir.getIrSymbolsForSealedSubclasses()
        } else {
            emptyList()
        }
    }

    override var thisReceiver: IrValueParameter? by lazyVar(lock) {
        symbolTable.enterScope(this)
        val typeArguments = fir.typeParameters.map {
            IrSimpleTypeImpl(
                classifierStorage.getCachedIrTypeParameter(it.symbol.fir)!!.symbol,
                hasQuestionMark = false, arguments = emptyList(), annotations = emptyList()
            )
        }
        val receiver = declareThisReceiverParameter(
            thisType = IrSimpleTypeImpl(symbol, hasQuestionMark = false, arguments = typeArguments, annotations = emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        symbolTable.leaveScope(this)
        receiver
    }

    override var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>?
        get() = computeValueClassRepresentation(fir)
        set(_) = mutationNotSupported()

    private val fakeOverridesByName = mutableMapOf<Name, Collection<IrDeclaration>>()

    fun getFakeOverridesByName(name: Name): Collection<IrDeclaration> = fakeOverridesByName.getOrPut(name) {
        fakeOverrideGenerator.generateFakeOverridesForName(this@Fir2IrLazyClass, name, fir)
            .also(converter::bindFakeOverridesOrPostpone)
    }

    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> by lazyVar(lock) {
        val result = mutableListOf<IrDeclaration>()
        // NB: it's necessary to take all callables from scope,
        // e.g. to avoid accessing un-enhanced Java declarations with FirJavaTypeRef etc. inside
        val scope = fir.unsubstitutedScope()
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
                    result += classifierStorage.getOrCreateIrClass(declaration.symbol)
                }
            }
        }

        if (fir.classKind == ClassKind.ENUM_CLASS) {
            for (declaration in fir.declarations) {
                if (declaration is FirEnumEntry && shouldBuildStub(declaration)) {
                    result += classifierStorage.getOrCreateIrEnumEntry(declaration, this, origin)
                }
            }
        }

        val ownerLookupTag = fir.symbol.toLookupTag()

        fun addDeclarationsFromScope(scope: FirContainingNamesAwareScope?) {
            if (scope == null) return
            for (name in scope.getCallableNames()) {
                scope.processFunctionsByName(name) l@{ symbol ->
                    when {
                        !shouldBuildStub(symbol.fir) -> {}
                        else -> {
                            // Lazy declarations are created together with their symbol, so it's safe to take the owner here
                            @OptIn(UnsafeDuringIrConstructionAPI::class)
                            result += declarationStorage.getIrFunctionSymbol(symbol, lookupTag).owner
                        }
                    }
                }
                scope.processPropertiesByName(name) l@{ symbol ->
                    when {
                        symbol is FirFieldSymbol && (symbol.isStatic || symbol.containingClassLookupTag() == ownerLookupTag) -> {
                            result += declarationStorage.getOrCreateIrField(symbol.fir, this)
                        }
                        !shouldBuildStub(symbol.fir) -> {}
                        symbol !is FirPropertySymbol -> {}
                        else -> {
                            // Lazy declarations are created together with their symbol, so it's safe to take the owner here
                            @OptIn(UnsafeDuringIrConstructionAPI::class)
                            result += declarationStorage.getIrPropertySymbol(symbol, lookupTag).owner as IrProperty
                        }
                    }
                }
            }
        }

        addDeclarationsFromScope(scope)
        addDeclarationsFromScope(fir.staticScope(session, scopeSession))

        with(classifierStorage) {
            result.addAll(getFieldsWithContextReceiversForClass(this@Fir2IrLazyClass, fir))
        }

        result
    }

    private fun shouldBuildStub(fir: FirDeclaration): Boolean {
        if (fir is FirCallableDeclaration && fir.originalOrSelf().origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk) return false
        if (fir !is FirMemberDeclaration) return true
        return when {
            fir is FirConstructor -> isObject || isEnumClass || !Visibilities.isPrivate(fir.visibility) // This special case seams to be not needed anymore - KT-65172
            fir is FirCallableDeclaration && fir.isFakeOverride(this.fir) -> session.visibilityChecker.isVisibleForOverriding(
                this.fir.moduleData,
                this.fir.classId.packageFqName,
                fir
            )
            else -> !Visibilities.isPrivate(fir.visibility)
        }
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val moduleName: String?
        get() = fir.moduleName

    override val isNewPlaceForBodyGeneration: Boolean
        get() = fir.isNewPlaceForBodyGeneration == true

    private var irLoaded: Boolean? = null

    override fun loadIr(): Boolean {
        assert(parent is IrPackageFragment)
        return irLoaded ?: extensions.deserializeToplevelClass(this, this).also { irLoaded = it }
    }
}
