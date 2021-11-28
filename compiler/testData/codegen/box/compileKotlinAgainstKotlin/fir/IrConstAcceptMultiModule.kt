// TARGET_BACKEND: JVM
// MODULE: lib
// WITH_STDLIB
// FILE: A.kt

abstract class IrConst<T> : IrExpression(), IrExpressionWithCopy {
    abstract val kind: IrConstKind<T>
    abstract val value: T

    abstract override fun copy(): IrConst<T>
    abstract fun copyWithOffsets(startOffset: Int, endOffset: Int): IrConst<T>
}

sealed class IrConstKind<T>(val asString: kotlin.String) {
    @Suppress("UNCHECKED_CAST")
    fun valueOf(aConst: IrConst<*>) =
        (aConst as IrConst<T>).value

    object Null : IrConstKind<Nothing?>("Null")
    object Boolean : IrConstKind<kotlin.Boolean>("Boolean")
    object Char : IrConstKind<kotlin.Char>("Char")
    object Byte : IrConstKind<kotlin.Byte>("Byte")
    object Short : IrConstKind<kotlin.Short>("Short")
    object Int : IrConstKind<kotlin.Int>("Int")
    object Long : IrConstKind<kotlin.Long>("Long")
    object String : IrConstKind<kotlin.String>("String")
    object Float : IrConstKind<kotlin.Float>("Float")
    object Double : IrConstKind<kotlin.Double>("Double")

    override fun toString() = asString
}

interface IrType

abstract class IrExpression : IrElementBase(), IrStatement, IrVarargElement, IrAttributeContainer {
    @Suppress("LeakingThis")
    override var attributeOwnerId: IrAttributeContainer = this

    abstract var type: IrType

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrExpression =
        accept(transformer, data) as IrExpression

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // No children by default
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // No children by default
    }
}

interface IrExpressionWithCopy {
    fun copy(): IrExpression
}

interface IrAttributeContainer : IrElement {
    var attributeOwnerId: IrAttributeContainer
}

abstract class IrElementBase : IrElement

interface IrStatement : IrElement

interface IrVarargElement : IrElement

interface IrElement {
    val startOffset: Int
    val endOffset: Int

    fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R

    fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D): Unit

    fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement =
        accept(transformer, data)

    fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D): Unit
}

interface IrElementVisitor<out R, in D>

interface IrElementTransformer<in D> : IrElementVisitor<IrElement, D>

// MODULE: main(lib)
// WITH_STDLIB
// FILE: B.kt

fun foo(cases: Collection<IrConst<*>>, exprTransformer: IrElementTransformer<Any>, context: Any) {
    cases.map {
        it.accept(exprTransformer, context)
    }
}

fun box(): String {
    return "OK"
}
