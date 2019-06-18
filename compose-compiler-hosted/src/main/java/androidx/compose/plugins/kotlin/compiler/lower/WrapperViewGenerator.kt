package androidx.compose.plugins.kotlin.compiler.lower
/*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.kotlin.ComponentMetadata
import androidx.compose.plugins.kotlin.buildWithScope
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

fun generateWrapperView(context: GeneratorContext, componentMetadata: ComponentMetadata): IrClass {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val wrapperViewIrClass = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, syntheticClassDescriptor)

    wrapperViewIrClass.createParameterDeclarations()
    val wrapperViewReceiverSymbol = wrapperViewIrClass.thisReceiver!!
    wrapperViewIrClass.declarations.add(generateConstructor(wrapperViewReceiverSymbol, context, componentMetadata))
    wrapperViewIrClass.declarations.addAll(generateProperties(context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateOnAttachFunction(wrapperViewReceiverSymbol, context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateOnDetachFunction(wrapperViewReceiverSymbol, context, componentMetadata))
    wrapperViewIrClass.declarations.add(generateOnPreDrawFunction(wrapperViewReceiverSymbol, context, componentMetadata))
    wrapperViewIrClass.declarations.addAll(generateAttributeSetterFunctions(context, componentMetadata))

    return wrapperViewIrClass
}

private fun generateConstructor(wrapperViewAsThisReceiver: IrValueParameter, context: GeneratorContext, componentMetadata: ComponentMetadata): IrConstructor {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    return context.symbolTable.declareConstructor(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        syntheticClassDescriptor.unsubstitutedPrimaryConstructor
    )
        .buildWithScope(context) { constructor ->
            constructor.createParameterDeclarations()
            context.symbolTable.introduceValueParameter(wrapperViewAsThisReceiver)
            val getThisExpr = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver.symbol)

            val statements = mutableListOf<IrStatement>()
            val superConstructor =
                context.symbolTable.referenceConstructor(componentMetadata.wrapperViewDescriptor.getSuperClassNotAny()!!.constructors.single { it.valueParameters.size == 1 })
            val superCall = IrDelegatingConstructorCallImpl(-1, -1, superConstructor, superConstructor.descriptor, 0).apply {
                putValueArgument(0, IrGetValueImpl(-1, -1, constructor.valueParameters[0].symbol))
            }

            statements.add(superCall)


            val linearLayoutClass =
                context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.widget.LinearLayout")))!!
            val linearLayoutParamsClass = linearLayoutClass.unsubstitutedMemberScope.getContributedClassifier(
                Name.identifier("LayoutParams"),
                NoLookupLocation.FROM_BACKEND
            )!! as ClassDescriptor
            val linearLayoutParamsConstructor =
                context.symbolTable.referenceConstructor(linearLayoutParamsClass.constructors.single { it.valueParameters.size == 2 && it.valueParameters[0].type == context.moduleDescriptor.builtIns.intType }!!)

            val layoutParams = IrCallImpl(
                -1, -1, linearLayoutParamsClass.defaultType,
                linearLayoutParamsConstructor,
                linearLayoutParamsConstructor.descriptor, null
            )
            layoutParams.putValueArgument(
                0, IrGetFieldImpl(
                    -1, -1, context.symbolTable.referenceField(
                        linearLayoutParamsClass.staticScope.getContributedVariables(
                            Name.identifier("MATCH_PARENT"), NoLookupLocation.FROM_BACKEND
                        ).single()
                    )
                )
            )
            layoutParams.putValueArgument(
                1, IrGetFieldImpl(
                    -1, -1, context.symbolTable.referenceField(
                        linearLayoutParamsClass.staticScope.getContributedVariables(
                            Name.identifier("WRAP_CONTENT"), NoLookupLocation.FROM_BACKEND
                        ).single()
                    )
                )
            )

            val setLayoutParamsFunction = linearLayoutClass.unsubstitutedMemberScope.getContributedFunctions(
                Name.identifier("setLayoutParams"),
                NoLookupLocation.FROM_BACKEND
            ).single()
            statements.add(IrInstanceInitializerCallImpl(-1, -1, context.symbolTable.referenceClass(syntheticClassDescriptor)))
            val setLayoutParamsCall = org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl(
                -1, -1, context.moduleDescriptor.builtIns.unitType,
                context.symbolTable.referenceSimpleFunction(setLayoutParamsFunction),
                setLayoutParamsFunction, null
            )
            setLayoutParamsCall.dispatchReceiver = getThisExpr
            setLayoutParamsCall.putValueArgument(0, layoutParams)
            statements.add(setLayoutParamsCall)


            val componentInstanceProperty = syntheticClassDescriptor.componentInstanceField
            val componentConstructorDescriptor = componentMetadata.descriptor.unsubstitutedPrimaryConstructor!!
            val componentConstructor = context.symbolTable.referenceConstructor(componentConstructorDescriptor)
            val componentConstructorCall = IrCallImpl(
                -1, -1, componentMetadata.descriptor.defaultType,
                componentConstructor,
                componentConstructorDescriptor, null
            )
            // this.componentInstance = ComponentClass()
            statements.add(
                IrSetFieldImpl(
                    -1,
                    -1,
                    context.symbolTable.referenceField(componentInstanceProperty),
                    getThisExpr,
                    componentConstructorCall
                )
            )

            // this.dirty = true
            statements.add(
                IrSetFieldImpl(
                    -1, -1,
                    context.symbolTable.referenceField(syntheticClassDescriptor.dirtyField),
                    getThisExpr,
                    IrConstImpl.boolean(-1, -1, context.builtIns.booleanType, true)
                )
            )

            val ccClass =
                context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(ComposeUtils.composeFqName("CompositionContext")))!!

            val ccCreateDescriptor = ccClass
                .companionObjectDescriptor
                ?.unsubstitutedMemberScope
                ?.getContributedFunctions(Name.identifier("create"), NoLookupLocation.FROM_BACKEND)
                ?.single()!!

            val ccCreateCall = IrCallImpl(
                -1, -1,
                context.symbolTable.referenceSimpleFunction(ccCreateDescriptor),
                ccCreateDescriptor
            )

            ccCreateCall.dispatchReceiver = IrGetObjectValueImpl(
                -1, -1,
                ccClass.companionObjectDescriptor!!.defaultType,
                context.symbolTable.referenceClass(ccClass.companionObjectDescriptor!!)
            )

            // context (Context)
            ccCreateCall.putValueArgument(0, IrGetValueImpl(-1, -1, constructor.valueParameters[0].symbol))

            // this (ViewGroup)
            ccCreateCall.putValueArgument(1, getThisExpr)

            // this.componentInstance (Component)
            ccCreateCall.putValueArgument(
                2, IrGetFieldImpl(
                    -1, -1,
                    context.symbolTable.referenceField(componentInstanceProperty),
                    getThisExpr
                )
            )

            // this.compositionContext = CompositionContext.create(context, this, instance)
            statements.add(
                IrSetFieldImpl(
                    -1, -1,
                    context.symbolTable.referenceField(syntheticClassDescriptor.compositionContextField),
                    getThisExpr,
                    ccCreateCall
                )
            )

            constructor.body = IrBlockBodyImpl(-1, -1, statements)
        }
}

private fun generateOnAttachFunction(wrapperViewAsThisReceiver: IrValueParameter, context: GeneratorContext, componentMetadata: ComponentMetadata): IrFunction {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val functionDescriptor = syntheticClassDescriptor.onAttachDescriptor
    val viewDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.View")))!!
        val viewGroupDescriptor =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewGroup")))!!
    val viewTreeObserverDescriptor =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewTreeObserver")))!!
    return context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            context.symbolTable.introduceValueParameter(wrapperViewAsThisReceiver)
            val getThisExpr = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver.symbol)

            val superFunction = viewGroupDescriptor.unsubstitutedMemberScope.getContributedFunctions(
                Name.identifier("onAttachedToWindow"),
                NoLookupLocation.FROM_BACKEND
            ).single()
            val superCall = IrCallImpl(
                -1, -1,
                context.moduleDescriptor.builtIns.unitType,
                context.symbolTable.referenceSimpleFunction(superFunction),
                superFunction
                , null, null, context.symbolTable.referenceClass(syntheticClassDescriptor.getSuperClassNotAny()!!)
            )
            superCall.dispatchReceiver = getThisExpr

            val getViewTreeObserverFunction = viewDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("getViewTreeObserver"), NoLookupLocation.FROM_BACKEND)
                .single()
            val getViewTreeObserverCall = IrCallImpl(
                -1, -1,
                context.symbolTable.referenceSimpleFunction(getViewTreeObserverFunction)
            )
            getViewTreeObserverCall.dispatchReceiver = getThisExpr

            val addOnPreDrawListenerFunction = viewTreeObserverDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("addOnPreDrawListener"), NoLookupLocation.FROM_BACKEND)
                .single()

            val addOnPreDrawListenerCall = IrCallImpl(
                -1, -1,
                context.symbolTable.referenceSimpleFunction(addOnPreDrawListenerFunction)
            )

            addOnPreDrawListenerCall.dispatchReceiver = getViewTreeObserverCall
            addOnPreDrawListenerCall.putValueArgument(0, getThisExpr)

            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(superCall, addOnPreDrawListenerCall))
        }
}

private fun generateOnDetachFunction(wrapperViewAsThisReceiver: IrValueParameter, context: GeneratorContext, componentMetadata: ComponentMetadata): IrFunction {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val functionDescriptor = syntheticClassDescriptor.onDetachDescriptor
    val viewDescriptor = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.View")))!!
    val viewGroupDescriptor =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewGroup")))!!
    val viewTreeObserverDescriptor =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewTreeObserver")))!!
    return context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            context.symbolTable.introduceValueParameter(wrapperViewAsThisReceiver)
            val getThisExpr = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver.symbol)

            val superFunction = viewGroupDescriptor.unsubstitutedMemberScope.getContributedFunctions(
                Name.identifier("onDetachedFromWindow"),
                NoLookupLocation.FROM_BACKEND
            ).single()
            val superCall = IrCallImpl(
                -1, -1,
                context.moduleDescriptor.builtIns.unitType,
                context.symbolTable.referenceSimpleFunction(superFunction),
                superFunction
                , null, null, context.symbolTable.referenceClass(syntheticClassDescriptor.getSuperClassNotAny()!!)
            )
            superCall.dispatchReceiver = getThisExpr

            val getViewTreeObserverFunction = viewDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("getViewTreeObserver"), NoLookupLocation.FROM_BACKEND)
                .single()
            val getViewTreeObserverCall = IrCallImpl(
                -1, -1,
                context.symbolTable.referenceSimpleFunction(getViewTreeObserverFunction)
            )
            getViewTreeObserverCall.dispatchReceiver = getThisExpr

            val removeOnPreDrawListenerFunction = viewTreeObserverDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("removeOnPreDrawListener"), NoLookupLocation.FROM_BACKEND)
                .single()

            val removeOnPreDrawListenerCall = IrCallImpl(
                -1, -1,
                context.symbolTable.referenceSimpleFunction(removeOnPreDrawListenerFunction)
            )

            removeOnPreDrawListenerCall.dispatchReceiver = getViewTreeObserverCall
            removeOnPreDrawListenerCall.putValueArgument(0, getThisExpr)

            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(superCall, removeOnPreDrawListenerCall))
        }
}

private fun generateOnPreDrawFunction(wrapperViewAsThisReceiver: IrValueParameter, context: GeneratorContext, componentMetadata: ComponentMetadata): IrFunction {
    val syntheticClassDescriptor = componentMetadata.wrapperViewDescriptor
    val functionDescriptor = syntheticClassDescriptor.onPreDraw
    val compositionContextDescriptor =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(ComposeUtils.composeFqName("CompositionContext")))!!
    return context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
        .buildWithScope(context) { irFunction ->
            irFunction.createParameterDeclarations()
            context.symbolTable.introduceValueParameter(wrapperViewAsThisReceiver)
            val getThisExpr = IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver.symbol)


            val getDirtyCall = IrGetFieldImpl(
                -1, -1,
                context.symbolTable.referenceField(syntheticClassDescriptor.dirtyField),
                getThisExpr
            )

            val setDirtyFalseCall = IrSetFieldImpl(
                -1, -1,
                context.symbolTable.referenceField(syntheticClassDescriptor.dirtyField),
                getThisExpr,
                IrConstImpl.boolean(-1, -1, context.builtIns.booleanType, false)
            )

            val getCompositionContextCall = IrGetFieldImpl(
                -1, -1,
                context.symbolTable.referenceField(syntheticClassDescriptor.compositionContextField),
                getThisExpr
            )

            val recomposeFromRootFunction = compositionContextDescriptor
                .unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("recompose"), NoLookupLocation.FROM_BACKEND)
                .single()
            val recomposeFromRootExpr = IrCallImpl(
                -1, -1,
                context.symbolTable.referenceFunction(recomposeFromRootFunction)
            ).apply {
                dispatchReceiver = getCompositionContextCall
                putValueArgument(0, IrGetFieldImpl(
                    -1,
                    -1,
                    context.symbolTable.referenceField(syntheticClassDescriptor.componentInstanceField),
                    getThisExpr
                ))
            }

            val ifDirtyExpr = IrIfThenElseImpl(
                -1, -1,
                context.builtIns.unitType,
                getDirtyCall,

                IrBlockImpl(
                    -1, -1,
                    context.builtIns.unitType,
                    null,
                    listOf(recomposeFromRootExpr, setDirtyFalseCall)
                )
            )

            val returnTrue = IrReturnImpl(-1, -1, irFunction.symbol, IrConstImpl.boolean(-1, -1, context.builtIns.booleanType, true))

            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(ifDirtyExpr, returnTrue))
        }
}

private fun generateAttributeSetterFunctions(context: GeneratorContext, componentMetadata: ComponentMetadata): Collection<IrFunction> {
    val output = mutableListOf<IrFunction>()
    for (functionDescriptor in componentMetadata.wrapperViewDescriptor.setterMethodDescriptors) {
        val irFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, functionDescriptor)
            .buildWithScope(context) { irFunction ->
                irFunction.createParameterDeclarations()
                val wrapperViewAsThisReceiver = irFunction.dispatchReceiverParameter!!.symbol

                val componentInstanceField = componentMetadata.wrapperViewDescriptor.componentInstanceField
                context.symbolTable.introduceValueParameter(irFunction.valueParameters[0])

                val componentAttribute = componentMetadata.getAttributeDescriptors()
                    .single({ it.name.identifier == ComposeUtils.propertyNameFromSetterMethod(irFunction.name.identifier) })

                val componentInstance = IrGetFieldImpl(
                    -1,
                    -1,
                    context.symbolTable.referenceField(componentInstanceField),
                    IrGetValueImpl(-1, -1, wrapperViewAsThisReceiver)
                )

                val newAttributeValue =
                    IrGetValueImpl(-1, -1, context.symbolTable.referenceValueParameter(functionDescriptor.valueParameters[0]))

                val setAttributeOnComponentInstruction =
                    if (componentAttribute.setter != null) {
                        val setAttributeOnComponentCall = IrSetterCallImpl(
                            -1,
                            -1,
                            context.symbolTable.referenceFunction(componentAttribute.setter!!),
                            componentAttribute.setter!!,
                            0
                        )
                        setAttributeOnComponentCall.dispatchReceiver = componentInstance
                        setAttributeOnComponentCall.putValueArgument(0, newAttributeValue)
                        setAttributeOnComponentCall
                    } else {
                        IrSetFieldImpl(-1, -1, context.symbolTable.referenceField(componentAttribute), componentInstance, newAttributeValue)
                    }

                // TODO: Invalidate the WrapperView using Leland's new WrapperView API (pending commit of that API)

                irFunction.body = IrBlockBodyImpl(-1, -1, listOf(setAttributeOnComponentInstruction /*, invalidateCall */))
            }
        output.add(irFunction)
    }
    return output
}


private fun generateProperties(context: GeneratorContext, componentMetadata: ComponentMetadata) = listOf(
    context.symbolTable.declareField(
        -1, -1,
        IrDeclarationOrigin.DEFINED,
        componentMetadata
            .wrapperViewDescriptor
            .componentInstanceField
    ),
    context.symbolTable.declareField(
        -1, -1,
        IrDeclarationOrigin.DEFINED,
        componentMetadata
            .wrapperViewDescriptor
            .compositionContextField
    ),
    context.symbolTable.declareField(
        -1, -1,
        IrDeclarationOrigin.DEFINED,
        componentMetadata
            .wrapperViewDescriptor
            .dirtyField
    )
)
        */
