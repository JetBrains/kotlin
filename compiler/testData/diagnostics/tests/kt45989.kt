// FIR_IDENTICAL
// !FORCE_RENDER_ARGUMENTS

interface IrElement

fun IrElement.dumpKotlinLike(options: String = ""): String = ""
fun IrElement.dump(normalizeNames: Boolean = false): String = ""

fun foo(dumpStrategy: String) {
    // FE 1.0: Ok, FIR: type mismatch, expected IrElement.() -> String, actual IrElement.(String & Boolean) -> String
    val dump: IrElement.() -> String = if (dumpStrategy == "KotlinLike") IrElement::dumpKotlinLike else IrElement::dump
}
