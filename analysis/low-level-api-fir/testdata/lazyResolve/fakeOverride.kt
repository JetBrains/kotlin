fun ex<caret>pression(expression: IrConst<*>) {
    when (val kind = expression.kind) {
        is Kind.Null -> kind.valueOf(expression)
        else -> {}
    }
}

sealed class Kind<T>(val asString: String) {
    @Suppress("UNCHECKED_CAST")
    fun valueOf(aConst: IrConst<*>) = (aConst as IrConst<T>).value
    object Null : Kind<Nothing?>("Null")
    override fun toString() = asString
}

abstract class IrConst<T> {
    abstract var kind: Kind<T>
    abstract var value: T
}
