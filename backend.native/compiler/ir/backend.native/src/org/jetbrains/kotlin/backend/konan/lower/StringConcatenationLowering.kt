package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * This lowering pass replaces [IrStringConcatenation]s with StringBuilder appends.
 */
class StringConcatenationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(StringConcatenationTransformer(this))
    }
}

private class StringConcatenationTransformer(val lower: StringConcatenationLowering) : IrElementTransformerVoid() {

    private val buildersStack = mutableListOf<IrBuilderWithScope>()
    private val context = lower.context
    private val irBuiltIns = context.irBuiltIns

    private val typesWithSpecialAppendFunction = irBuiltIns.primitiveIrTypes + irBuiltIns.stringType

    private val nameToString = Name.identifier("toString")
    private val nameAppend = Name.identifier("append")

    private val stringBuilder = context.ir.symbols.stringBuilder.owner

    //TODO: calculate and pass string length to the constructor.
    private val constructor = stringBuilder.constructors.single {
        it.valueParameters.size == 0
    }

    private val toStringFunction = stringBuilder.functions.single {
        it.valueParameters.size == 0 && it.name == nameToString
    }

    private val defaultAppendFunction = stringBuilder.functions.single {
        it.name == nameAppend &&
                it.valueParameters.size == 1 &&
                it.valueParameters.single().type.isNullableAny()
    }

    private val appendFunctions: Map<IrType, IrSimpleFunction?> =
            typesWithSpecialAppendFunction.map { type ->
                type to stringBuilder.functions.toList().atMostOne {
                    it.name == nameAppend && it.valueParameters.singleOrNull()?.type == type
                }
            }.toMap()

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction {
        return appendFunctions[type] ?: defaultAppendFunction
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        assert(!buildersStack.isEmpty())

        expression.transformChildrenVoid(this)
        val blockBuilder = buildersStack.last()

        val arguments = expression.arguments
        return when (arguments.size) {
            0 -> blockBuilder.irString("")
            1 -> {
                val argument = arguments[0]
                if (argument.type.isNullable())
                    blockBuilder.irCall(context.ir.symbols.extensionToString).apply {
                        extensionReceiver = argument
                    }
                else blockBuilder.irCall(
                        context.irBuiltIns.anyClass.functions
                                .single { it.owner.name.asString() == "toString" }).apply {
                    dispatchReceiver = argument
                }
            }
            else -> {
                blockBuilder.irBlock(expression) {
                    val stringBuilderImpl = createTmpVariable(irCall(constructor))
                    expression.arguments.forEach { arg ->
                        val appendFunction = typeToAppendFunction(arg.type)
                        +irCall(appendFunction).apply {
                            dispatchReceiver = irGet(stringBuilderImpl)
                            putValueArgument(0, arg)
                        }
                    }
                    +irCall(toStringFunction).apply {
                        dispatchReceiver = irGet(stringBuilderImpl)
                    }
                }
            }
        }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration !is IrSymbolDeclaration<*>) {
            return super.visitDeclaration(declaration)
        }

        with(declaration) {
            buildersStack.add(
                    context.createIrBuilder(declaration.symbol, startOffset, endOffset)
            )
            transformChildrenVoid(this@StringConcatenationTransformer)
            buildersStack.removeAt(buildersStack.lastIndex)
            return this@with
        }
    }
}
