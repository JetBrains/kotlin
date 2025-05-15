import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability

class IrTrackedExpressionType(val delegate: IrSimpleType) : IrSimpleType() {
    override val classifier: IrClassifierSymbol
        get() = delegate.classifier
    override val nullability: SimpleTypeNullability
        get() = delegate.nullability

    override val arguments: List<IrTypeArgument>
        get() {
            recordTrace()
            return delegate.arguments
        }

    override val abbreviation: IrTypeAbbreviation?
        get() {
            recordTrace()
            return delegate.abbreviation
        }

    override val annotations: List<IrConstructorCall>
        get() {
            recordTrace()
            return delegate.annotations
        }

    override fun equals(other: Any?): Boolean = delegate.equals(other)

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    private fun recordRead() {

    }

    companion object {
        private val reportedTraces = hashSetOf<List<StackTraceElement>>()

        internal fun recordDeclarationTypeChanged(newType: IrType?) {
            if (newType is IrTrackedExpressionType) {
                recordTrace()
            }
        }

        internal fun recordTrace() {
            var stack = Throwable().stackTrace.drop(1)
            if (stack[2].methodName != "removeAnnotations"
                && stack[1].className != "org.jetbrains.kotlin.ir.util.RenderIrElementKt"
                && stack[1].className != "org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid"
                && stack[1].className != "org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper"
                && !(stack[1].className == "org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImplKt" && stack[1].methodName == "toBuilder")
                && !(stack[1].className == "org.jetbrains.kotlin.ir.util.IrTypeParameterRemapper" && stack[1].methodName == "remapType")
                && !(stack[1].className == "org.jetbrains.kotlin.ir.util.IrUtilsKt" && stack[1].methodName == "remapTypeParameters")
            ) {
                var regularFrames = 7
                stack = stack.take(20)
                    .groupBy { it.className + it.methodName.removeSuffix("\$default") }.map { it.value[0] }
                    .takeWhile {
                        it.className.startsWith("org.jetbrains.kotlin.types.") ||
                                it.className.startsWith("org.jetbrains.kotlin.ir.types.") || regularFrames-- > 0
                    }
                    .takeWhile {
                        !it.className.startsWith("org.jetbrains.kotlin.ir.visitors.") &&
                            !(it.className.startsWith("org.jetbrains.kotlin.ir.") && (it.methodName == "accept" || it.methodName == "transform"))
                    }
                if (reportedTraces.add(stack.dropLast(2))) {
                    println(stack.joinToString("\n   "))
                }
            }
        }
    }
}

fun IrType.wrap() = if (this is IrSimpleType && this !is IrTrackedExpressionType) {
    IrTrackedExpressionType(this)
} else this

fun IrType.unwrap() = if (this is IrTrackedExpressionType) delegate else this