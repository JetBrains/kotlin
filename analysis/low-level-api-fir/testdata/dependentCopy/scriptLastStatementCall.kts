// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtCallExpression
class ScriptClass {
    val memberProperty = 4
}

fun scriptFunction() = 42
fun unusedScriptFunction(p: String) = 22

scriptFunction()

fun foo(i: Int, action: (Int) -> Unit) {
    action(i)
}

<caret>foo(scriptFunction()) {
    scriptFunction()
}
