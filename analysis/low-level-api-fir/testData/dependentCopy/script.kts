// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtScript

<caret>class ScriptClass {
    val memberProperty = 4
}

fun scriptFunction() = 42
fun unusedScriptFunction(p: String) = 22

scriptFunction()

fun foo(i: Int, action: (Int) -> Unit) {
    action(i)
}

foo(scriptFunction()) {
    scriptFunction()
}
