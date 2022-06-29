open class IrElement

fun IrElement.dumpKotlinLike(options: String = ""): String = "O"

fun IrElement.dump(normalizeNames: Boolean = false, stableOrder: Boolean = false): String = "K"

fun dump(data: IrElement, dumpStrategy: String): String {
    val dump: IrElement.() -> String = if (dumpStrategy == "KotlinLike") IrElement::dumpKotlinLike else IrElement::dump
    return data.dump()
}
