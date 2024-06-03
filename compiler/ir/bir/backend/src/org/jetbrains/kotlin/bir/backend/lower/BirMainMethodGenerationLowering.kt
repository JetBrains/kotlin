/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.CompressedSourceSpan
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.*
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirSimpleFunctionImpl
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.get
import org.jetbrains.kotlin.bir.symbols.ownerIfBound
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.typeWith
import org.jetbrains.kotlin.bir.types.utils.typeWithParameters
import org.jetbrains.kotlin.bir.util.constructors
import org.jetbrains.kotlin.bir.util.isFileClass
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

context(JvmBirBackendContext)
class BirMainMethodGenerationLowering : BirLoweringPhase() {
    override fun lower(module: BirModuleFragment) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)) return

        for (mainMethod in getAllElementsOfClass(BirSimpleFunction, false)) {
            if (!(mainMethod.extensionReceiverParameter == null
                        && mainMethod.valueParameters.size <= 1
                        && mainMethod.typeParameters.isEmpty()
                        && mainMethod.getJvmName().asString() == "main"
                        && mainMethod.returnType.isUnit()
                        )
            ) continue

            val parentClass = mainMethod.parent as? BirClass ?: continue
            if (!parentClass.isFileClass) continue
            val isParametrized = mainMethod.isParameterizedMainMethod() ?: continue

            birBodyScope {
                var newMainMethod: BirSimpleFunction? = null
                if (isParametrized) {
                    if (mainMethod.isSuspend) {
                        newMainMethod = generateMainMethod()
                        newMainMethod.body = birBlockBody {
                            +birRunSuspend(mainMethod, newMainMethod.valueParameters[0], newMainMethod)
                        }
                    }
                } else {
                    if (mainMethod.isSuspend) {
                        newMainMethod = generateMainMethod()
                        newMainMethod.body = birBlockBody {
                            +birRunSuspend(mainMethod, null, newMainMethod)
                        }
                    } else {
                        newMainMethod = generateMainMethod()
                        newMainMethod.body = birBlockBody {
                            +birCall(mainMethod)
                        }
                    }
                }

                if (newMainMethod != null) {
                    parentClass.declarations += newMainMethod
                }
            }
        }
    }

    private fun BirSimpleFunction.getJvmName() =
        this[BirJvmNameLowering.JvmName] ?: name

    private fun BirSimpleFunction.isParameterizedMainMethod(): Boolean? {
        val parameter = valueParameters.singleOrNull()
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

    context(BirStatementBuilderScope)
    private fun birRunSuspend(target: BirSimpleFunction, args: BirValueParameter?, newMain: BirSimpleFunction): BirExpression {
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

            val functionClass = builtInSymbols.getJvmSuspendFunctionClass(0)

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
                overriddenSymbols += functionClass.owner.declarations.filterIsInstance<BirSimpleFunction>().single().symbol
                val invokeFunction = this@build
                body = birBodyScope {
                    returnTarget = invokeFunction
                    birBlockBody {
                        +birReturn(
                            birCall(target) {
                                if (args != null) {
                                    valueArguments[0] = birGetField(birGet(invokeFunction.dispatchReceiverParameter!!), argsField!!)
                                }
                            }
                        )
                    }
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
                    valueParameters += it
                }
            }

            body = birBodyScope {
                birBlockBody {
                    +birDelegatingConstructorCall(lambdaSuperClass.owner.constructors.single()) {
                        valueArguments[0] = birConst(0)
                    }
                    if (args != null) {
                        +birSetField(birGet(wrapperClass.thisReceiver!!), argsField!!.symbol, birGet(param!!))
                    }
                }
            }
        }
        wrapperClass.declarations += wrapperConstructor

        return birBlock {
            +wrapperClass
            +birCall(builtInSymbols.runSuspendFunction.owner) {
                valueArguments[0] = birCall(wrapperConstructor) {
                    if (args != null) {
                        valueArguments[0] = birGet(args)
                    }
                }
            }
        }
    }
}