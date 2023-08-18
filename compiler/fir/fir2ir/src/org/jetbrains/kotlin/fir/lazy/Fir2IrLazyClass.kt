/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbolInternals
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.DeserializableClass
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

class Fir2IrLazyClass(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirRegularClass,
    override val symbol: IrClassSymbol,
) : IrClass(), AbstractFir2IrLazyDeclaration<FirRegularClass>, Fir2IrTypeParametersContainer,
    IrMaybeDeserializedClass, DeserializableClass, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir, symbol)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
    override lateinit var typeParameters: List<IrTypeParameter>
    override lateinit var parent: IrDeclarationParent

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

    @OptIn(IrSymbolInternals::class)
    override val declarations: MutableList<IrDeclaration> by lazyVar(lock) {
        val result = mutableListOf<IrDeclaration>()
        // NB: it's necessary to take all callables from scope,
        // e.g. to avoid accessing un-enhanced Java declarations with FirJavaTypeRef etc. inside
        val scope = fir.unsubstitutedScope()
        scope.processDeclaredConstructors {
            if (shouldBuildStub(it.fir)) {
                result += declarationStorage.getIrConstructorSymbol(it).owner
            }
        }

        for (name in scope.getClassifierNames()) {
            scope.processClassifiersByName(name) {
                val declaration = it.fir as? FirRegularClass ?: return@processClassifiersByName
                if (declaration.classId.outerClassId == fir.classId && shouldBuildStub(declaration)) {
                    val nestedSymbol = classifierStorage.getIrClassSymbol(declaration.symbol)
                    result += nestedSymbol.owner
                }
            }
        }

        if (fir.classKind == ClassKind.ENUM_CLASS) {
            for (declaration in fir.declarations) {
                if (declaration is FirEnumEntry && shouldBuildStub(declaration)) {
                    result += declarationStorage.getIrValueSymbol(declaration.symbol).owner as IrDeclaration
                }
            }
        }

        val ownerLookupTag = fir.symbol.toLookupTag()

        fun addDeclarationsFromScope(scope: FirContainingNamesAwareScope?) {
            if (scope == null) return
            for (name in scope.getCallableNames()) {
                scope.processFunctionsByName(name) {
                    if (it.isSubstitutionOrIntersectionOverride) return@processFunctionsByName
                    if (!shouldBuildStub(it.fir)) return@processFunctionsByName
                    if (it.isStatic || it.dispatchReceiverClassLookupTagOrNull() == ownerLookupTag) {
                        if (it.isAbstractMethodOfAny()) {
                            return@processFunctionsByName
                        }
                        result += declarationStorage.getIrFunctionSymbol(it).owner
                    }
                }
                scope.processPropertiesByName(name) {
                    if (it.isSubstitutionOrIntersectionOverride) return@processPropertiesByName
                    if (!shouldBuildStub(it.fir)) return@processPropertiesByName
                    if (it is FirPropertySymbol && (it.isStatic || it.dispatchReceiverClassLookupTagOrNull() == ownerLookupTag)) {
                        result.addIfNotNull(
                            declarationStorage.getIrPropertySymbol(it).owner as? IrDeclaration
                        )
                    }
                }
            }
        }

        addDeclarationsFromScope(scope)
        addDeclarationsFromScope(fir.staticScope(session, scopeSession))

        with(classifierStorage) {
            result.addAll(getFieldsWithContextReceiversForClass(this@Fir2IrLazyClass, fir))
        }

        for (name in scope.getCallableNames()) {
            result += getFakeOverridesByName(name)
        }

        result
    }

    private fun shouldBuildStub(fir: FirDeclaration): Boolean =
        fir !is FirMemberDeclaration ||
                !Visibilities.isPrivate(fir.visibility) ||
                // This exception is needed for K/N caches usage
                (isObject && fir is FirConstructor) ||
                // Needed for enums
                (this.isEnumClass && fir is FirConstructor)

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val moduleName: String?
        get() = fir.moduleName

    override val isNewPlaceForBodyGeneration: Boolean
        get() = fir.isNewPlaceForBodyGeneration == true

    private fun FirNamedFunctionSymbol.isAbstractMethodOfAny(): Boolean {
        if (modality != Modality.ABSTRACT) return false
        return isMethodOfAny
    }

    private var irLoaded: Boolean? = null

    override fun loadIr(): Boolean {
        assert(parent is IrPackageFragment)
        return irLoaded ?: extensions.deserializeToplevelClass(this, this).also { irLoaded = it }
    }
}
