/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

class Fir2IrLazyClass(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirRegularClass,
    override val symbol: Fir2IrClassSymbol,
) : IrClass(), AbstractFir2IrLazyDeclaration<FirRegularClass, IrClass>, IrMaybeDeserializedClass, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
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
        set(_) {
            throw UnsupportedOperationException()
        }

    @Suppress("SetterBackingFieldAssignment")
    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override var modality: Modality
        get() = fir.modality!!
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override var attributeOwnerId: IrAttributeContainer
        get() = this
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override val kind: ClassKind
        get() = fir.classKind

    override val isCompanion: Boolean
        get() = fir.isCompanion

    override val isInner: Boolean
        get() = fir.isInner

    override val isData: Boolean
        get() = fir.isData

    override val isExternal: Boolean
        get() = fir.isExternal

    override val isValue: Boolean
        get() = fir.isInline

    override val isExpect: Boolean
        get() = fir.isExpect

    override val isFun: Boolean
        get() = fir.isFun

    override var superTypes: List<IrType> by lazyVar(lock) {
        fir.superTypeRefs.map { it.toIrType(typeConverter) }
    }

    override var sealedSubclasses: List<IrClassSymbol> by lazyVar(lock) {
        if (fir.isSealed) {
            fir.getIrSymbolsForSealedSubclasses(components)
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
            symbolTable,
            thisType = IrSimpleTypeImpl(symbol, hasQuestionMark = false, arguments = typeArguments, annotations = emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        symbolTable.leaveScope(this)
        receiver
    }

    override var inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>?
        get() = computeInlineClassRepresentation(fir)
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    private val fakeOverridesByName = mutableMapOf<Name, Collection<IrDeclaration>>()

    fun getFakeOverridesByName(name: Name): Collection<IrDeclaration> = fakeOverridesByName.getOrPut(name) {
        fakeOverrideGenerator.generateFakeOverridesForName(this@Fir2IrLazyClass, name, fir)
            .also(converter::bindFakeOverridesOrPostpone)
    }

    override val declarations: MutableList<IrDeclaration> by lazyVar(lock) {
        val result = mutableListOf<IrDeclaration>()
        // NB: it's necessary to take all callables from scope,
        // e.g. to avoid accessing un-enhanced Java declarations with FirJavaTypeRef etc. inside
        val scope = fir.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
        scope.processDeclaredConstructors {
            result += declarationStorage.getIrConstructorSymbol(it).owner
        }

        for (declaration in fir.declarations) {
            if (declaration is FirRegularClass) {
                val nestedSymbol = classifierStorage.getIrClassSymbol(declaration.symbol)
                result += nestedSymbol.owner
            }
        }

        // Handle generated methods for enum classes (values(), valueOf(String)).
        if (fir.classKind == ClassKind.ENUM_CLASS) {
            for (declaration in fir.declarations) {
                if (declaration !is FirSimpleFunction || !declaration.isStatic) continue
                // TODO we also come here for all deserialized / enhanced static enum members (with declaration.source == null).
                //  For such members we currently can't tell whether they are compiler-generated methods or not.
                // Note: we must drop declarations from Java here to avoid FirJavaTypeRefs inside
                if (declaration.source == null && declaration.origin != FirDeclarationOrigin.Java ||
                    declaration.source?.kind == KtFakeSourceElementKind.EnumGeneratedDeclaration
                ) {
                    result += declarationStorage.getIrFunctionSymbol(declaration.symbol).owner
                }
            }
        }

        val ownerLookupTag = fir.symbol.toLookupTag()
        for (name in scope.getCallableNames()) {
            scope.processFunctionsByName(name) {
                if (it.isSubstitutionOrIntersectionOverride) return@processFunctionsByName
                if (it.dispatchReceiverClassOrNull() == ownerLookupTag) {
                    if (it.isAbstractMethodOfAny()) {
                        return@processFunctionsByName
                    }
                    result += declarationStorage.getIrFunctionSymbol(it).owner
                }
            }
            scope.processPropertiesByName(name) {
                if (it.isSubstitutionOrIntersectionOverride) return@processPropertiesByName
                if (it is FirPropertySymbol && it.dispatchReceiverClassOrNull() == ownerLookupTag) {
                    result.addIfNotNull(declarationStorage.getIrPropertySymbol(it).owner as? IrDeclaration)
                }
            }
        }

        for (name in scope.getCallableNames()) {
            result += getFakeOverridesByName(name)
        }

        result
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val moduleName: String?
        get() = fir.moduleName

    override val isNewPlaceForBodyGeneration: Boolean
        get() = fir.isNewPlaceForBodyGeneration == true

    private fun FirNamedFunctionSymbol.isAbstractMethodOfAny(): Boolean {
        val fir = fir
        if (fir.modality != Modality.ABSTRACT) return false
        return when (fir.name) {
            OperatorNameConventions.EQUALS -> fir.valueParameters.singleOrNull()?.returnTypeRef?.isNullableAny == true
            OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING -> fir.valueParameters.isEmpty()
            else -> false
        }
    }
}
