// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG

interface Out<out E>
fun bar(arguments: Out<IrTypeArgument>) {}

interface IrTypeArgument

object IrStarProjectionImpl : IrTypeArgument

fun <T> MyOut(init: () -> T): Out<T> = TODO()

fun foo() {
    bar(MyOut { IrStarProjectionImpl })
}
