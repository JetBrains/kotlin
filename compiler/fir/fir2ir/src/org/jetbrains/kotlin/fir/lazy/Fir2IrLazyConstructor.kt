/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.backend.utils.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyConstructor(
    private val c: Fir2IrComponents,
    override var startOffset: Int,
    override var endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirConstructor,
    override val symbol: IrConstructorSymbol,
    parent: IrDeclarationParent,
) : IrConstructor(), AbstractFir2IrLazyDeclaration<FirConstructor>, Fir2IrTypeParametersContainer,
    Fir2IrComponents by c {
    init {
        this.parent = parent
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrAnnotation> by createLazyAnnotations()
    override lateinit var typeParameters: List<IrTypeParameter>

    override var isPrimary: Boolean
        get() = fir.isPrimary
        set(_) = mutationNotSupported()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor
        get() = symbol.descriptor

    override var isInline: Boolean
        get() = fir.isInline
        set(_) = mutationNotSupported()

    override var isExternal: Boolean
        get() = fir.isExternal
        set(_) = mutationNotSupported()

    override var isExpect: Boolean
        get() = fir.isExpect
        set(_) = mutationNotSupported()

    override var body: IrBody? = null

    override var name: Name
        get() = SpecialNames.INIT
        set(_) = mutationNotSupported()

    override var visibility: DescriptorVisibility = c.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var returnType: IrType by lazyVar(lock) {
        fir.returnTypeRef.toIrType()
    }

    override var attributeOwnerId: IrElement = this

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource

    @OptIn(DelicateIrParameterIndexSetter::class)
    override val parameterStorage: MutableList<IrValueParameter> by lazy {
        declarationStorage.enterScope(this.symbol)

        buildList {
            val containingClass = parent as? IrClass
            val outerClass = containingClass?.parentClassOrNull
            if (containingClass?.isInner == true && outerClass != null) {
                context(c) {
                    add(
                        this@Fir2IrLazyConstructor.declareThisReceiverParameter(
                            thisType = outerClass.thisReceiver!!.type,
                            thisOrigin = origin,
                            kind = IrParameterKind.DispatchReceiver
                        )
                    )
                }
            }

            callablesGenerator.addContextParametersTo(
                fir.contextParameters,
                this@Fir2IrLazyConstructor,
                this@buildList
            )

            fir.valueParameters.mapTo(this) { valueParameter ->
                val parentClass = parent as? IrClass
                callablesGenerator.createIrParameter(
                    valueParameter,
                    useStubForDefaultValueStub = parentClass?.classId != StandardClassIds.Enum,
                    forcedDefaultValueConversion = parentClass?.isAnnotationClass == true
                ).apply {
                    this.parent = this@Fir2IrLazyConstructor
                }
            }
        }.apply {
            declarationStorage.leaveScope(this@Fir2IrLazyConstructor.symbol)
        }.toMutableList()
    }
}
