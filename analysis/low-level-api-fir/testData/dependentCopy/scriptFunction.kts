class ScriptClass {
    val memberProperty = 4
}

fun scriptFunction() = 42
fun u<caret>nusedScriptFunction(p: String) = 22

scriptFunction()

fun foo(i: Int, action: (Int) -> Unit) {
    action(i)
}

foo(scriptFunction()) {
    scriptFunction()
}
