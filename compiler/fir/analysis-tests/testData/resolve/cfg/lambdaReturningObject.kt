// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG

interface Out<out E>
fun bar(arguments: Out<IrTypeArgument>) {}

interface IrTypeArgument

object IrStarProjectionImpl : IrTypeArgument

fun <T> MyOut(init: () -> T): Out<T> = TODO()

fun foo() {
    bar(MyOut { IrStarProjectionImpl })
}
