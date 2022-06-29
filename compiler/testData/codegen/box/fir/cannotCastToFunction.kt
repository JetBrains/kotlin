// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR

open class IrElement

fun IrElement.dumpKotlinLike(options: String = ""): String = "O"

fun IrElement.dump(normalizeNames: Boolean = false, stableOrder: Boolean = false): String = "K"

fun <T> select(flag: Boolean, a: T, b: T): T =
    if (flag) a else b

fun dump(data: IrElement, dumpStrategy: String): String {
    // Note: K1 reports TYPE_MISMATCH here (see also cannotCastToFunctionInIf.kt working both in K1/K2)
    val dump: IrElement.() -> String = select(dumpStrategy == "KotlinLike", IrElement::dumpKotlinLike, IrElement::dump)
    return data.dump()
}

fun box(): String {
    val element = IrElement()
    return dump(element, "KotlinLike") + dump(element, "OtherStrategy")
}
