// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-85825
class MyContext

fun createAction(): context(MyContext) () -> Unit = {}

context(_: MyContext)
fun usage1() {
    val action = createAction()
    action()
}

context(_: MyContext)
fun usage2() {
    val action = run { createAction() }
    action()
}

context(_: MyContext)
fun usage3() {
    val action = run { run { createAction() } }
    action()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, functionalType,
lambdaLiteral, localProperty, propertyDeclaration, typeWithContext */
