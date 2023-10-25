/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.jvm.getJvmNameFromAnnotation
import org.jetbrains.kotlin.bir.backend.utils.int
import org.jetbrains.kotlin.bir.builders.build
import org.jetbrains.kotlin.bir.builders.setCall
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirSimpleFunctionImpl
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.typeWith
import org.jetbrains.kotlin.bir.types.utils.typeWithParameters
import org.jetbrains.kotlin.bir.util.allParameters
import org.jetbrains.kotlin.bir.util.constructors
import org.jetbrains.kotlin.bir.util.getAnnotation
import org.jetbrains.kotlin.bir.util.isFileClass
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.Variance

context(JvmBirBackendContext)
class BirMainMethodGenerationLowering : BirLoweringPhase() {
    private val JvmNameAnnotation = birBuiltIns.findClass(DescriptorUtils.JVM_NAME)!!

    private val mainishFunctions = registerIndexKey<BirSimpleFunction>(false) { function ->
        function.getJvmName() == "main"
                && function.returnType.isUnit()
                && function.typeParameters.isEmpty()
    }

    override fun invoke(module: BirModuleFragment) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)) return

        compiledBir.getElementsWithIndex(mainishFunctions).forEach { mainMethod ->
            val parentClass = mainMethod.parent as? BirClass ?: return@forEach
            if (!parentClass.isFileClass) return@forEach
            val isParametrized = mainMethod.isParameterizedMainMethod() ?: return@forEach

            var newMainMethod: BirSimpleFunction? = null
            if (isParametrized) {
                if (!(mainMethod.extensionReceiverParameter == null && mainMethod.valueParameters.isEmpty())) return@forEach

                if (mainMethod.isSuspend) {
                    newMainMethod = generateMainMethod()
                    newMainMethod.body = BirBlockBodyImpl(SourceSpan.UNDEFINED).apply {
                        statements += birRunSuspend(mainMethod, newMainMethod!!.valueParameters[0], newMainMethod!!)
                    }
                }
            } else {
                if (mainMethod.isSuspend) {
                    newMainMethod = generateMainMethod()
                    newMainMethod.body = BirBlockBodyImpl(SourceSpan.UNDEFINED).apply {
                        statements += birRunSuspend(mainMethod, null, newMainMethod!!)
                    }
                } else {
                    newMainMethod = generateMainMethod()
                    newMainMethod.body = BirBlockBodyImpl(SourceSpan.UNDEFINED).apply {
                        statements += BirCall.build {
                            setCall(mainMethod)
                        }
                    }
                }
            }

            if (newMainMethod != null) {
                parentClass.declarations += newMainMethod
            }
        }
    }

    private fun BirSimpleFunction.getJvmName() =
        getAnnotation(JvmNameAnnotation)?.let { getJvmNameFromAnnotation(it) } ?: name.asString()

    private fun BirSimpleFunction.isParameterizedMainMethod(): Boolean? {
        val parameter = allParameters.singleOrNull()
        if (parameter != null) {
            if (!parameter.type.isArray() && !parameter.type.isNullableArray()) return null
            val argType = (parameter.type as BirSimpleType).arguments.first()
            when (argType) {
                is BirTypeProjection -> {
                    if (!((argType.variance != Variance.IN_VARIANCE) && argType.type.isStringClassType())) {
                        return null
                    }
                }
                is BirStarProjection -> return null
            }

            return true
        }

        return false
    }

    private fun generateMainMethod(): BirSimpleFunctionImpl {
        return BirSimpleFunction.build {
            name = Name.identifier("main")
            visibility = DescriptorVisibilities.PUBLIC
            returnType = birBuiltIns.unitType
            modality = Modality.OPEN
            origin = JvmLoweredDeclarationOrigin.GENERATED_EXTENDED_MAIN
            valueParameters += BirValueParameter.build {
                name = Name.identifier("args")
                type = birBuiltIns.arrayClass.typeWith(birBuiltIns.stringType)
            }
        }
    }

    private fun birRunSuspend(target: BirSimpleFunction, args: BirValueParameter?, newMain: BirSimpleFunction): BirBlockImpl {
        val lambdaSuperClass = builtInSymbols.lambdaClass
        val stringArrayType = birBuiltIns.arrayClass.typeWith(birBuiltIns.stringType)

        var argsField: BirField? = null
        val wrapperClass = BirClass.build {
            name = Name.special("<main-wrapper>")
            visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            modality = Modality.FINAL
            origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            val wrapperClass = this@build
            thisReceiver = BirValueParameter.build {
                name = SpecialNames.THIS
                origin = IrDeclarationOrigin.INSTANCE_RECEIVER
                index = UNDEFINED_PARAMETER_INDEX
                type = wrapperClass.typeWithParameters(typeParameters)
            }

            val functionClass = builtInSymbols.getJvmSuspendFunctionClass(0).owner

            superTypes += lambdaSuperClass.defaultType
            superTypes += functionClass.defaultType

            args?.let {
                argsField = BirField.build {
                    name = Name.identifier("args")
                    type = stringArrayType
                    visibility = DescriptorVisibilities.PRIVATE
                    origin = LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                }
                declarations += argsField!!
            }

            declarations += BirSimpleFunction.build {
                name = Name.identifier("invoke")
                returnType = birBuiltIns.anyNType
                isSuspend = true
                origin = IrDeclarationOrigin.DEFINED
                overriddenSymbols += functionClass.declarations.filterIsInstance<BirSimpleFunction>().single()
                val invokeFunction = this@build
                body = BirBlockBodyImpl(SourceSpan.UNDEFINED).apply {
                    val call = BirCall.build {
                        setCall(target)
                        if (args != null) {
                            valueArguments += BirGetFieldImpl(
                                SourceSpan.UNDEFINED,
                                argsField!!.type,
                                argsField!!,
                                null,
                                BirGetValueImpl(
                                    SourceSpan.UNDEFINED,
                                    invokeFunction.dispatchReceiverParameter!!.type,
                                    invokeFunction.dispatchReceiverParameter!!,
                                    null
                                ),
                                null,
                            )
                        }
                    }
                    statements += BirReturnImpl(SourceSpan.UNDEFINED, call.type, call, invokeFunction)
                }
            }
        }

        val wrapperConstructor = BirConstructor.build {
            isPrimary = true
            visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            returnType = wrapperClass.defaultType
            val param = args?.let {
                BirValueParameter.build {
                    name = Name.identifier("args")
                    type = stringArrayType
                    origin = IrDeclarationOrigin.DEFINED
                }.also {
                    valueParameters += it
                }
            }

            body = BirBlockBodyImpl(SourceSpan.UNDEFINED).apply {
                statements += BirDelegatingConstructorCallImpl(
                    SourceSpan.UNDEFINED,
                    lambdaSuperClass.defaultType,
                    lambdaSuperClass.owner.constructors.single(),
                    null,
                    null,
                    null,
                    emptyList(),
                    0
                ).apply {
                    valueArguments += BirConst.int(value = 0)
                }

                if (args != null) {
                    statements += BirSetFieldImpl(
                        SourceSpan.UNDEFINED,
                        argsField!!.type,
                        argsField!!,
                        null,
                        BirGetValueImpl(SourceSpan.UNDEFINED, wrapperClass.thisReceiver!!.type, wrapperClass.thisReceiver!!, null),
                        null,
                        BirGetValueImpl(SourceSpan.UNDEFINED, param!!.type, param, null),
                    )
                }
            }
        }
        wrapperClass.declarations += wrapperConstructor

        val block = BirBlockImpl(SourceSpan.UNDEFINED, birBuiltIns.unitType, null)
        block.statements += wrapperClass
        block.statements += BirCall.build {
            setCall(builtInSymbols.runSuspendFunction.owner)
            valueArguments += BirConstructorCall.build {
                setCall(wrapperConstructor)
                if (args != null) {
                    valueArguments += BirGetValueImpl(SourceSpan.UNDEFINED, args.type, args, null)
                }
            }
        }

        return block
    }
}