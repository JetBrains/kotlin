// RUN_PIPELINE_TILL: BACKEND
interface IrElement

fun IrElement.dumpKotlinLike(options: String = ""): String = ""
fun IrElement.dump(normalizeNames: Boolean = false): String = ""

fun foo(dumpStrategy: String) {
    val dump: IrElement.() -> String = if (dumpStrategy == "KotlinLike") IrElement::dumpKotlinLike else IrElement::dump
}

/* GENERATED_FIR_TAGS: callableReference, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, interfaceDeclaration, localProperty, propertyDeclaration, stringLiteral, typeWithExtension */
