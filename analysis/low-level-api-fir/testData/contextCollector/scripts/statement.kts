class ScriptClass {
    val memberProperty = 4
}

fun scriptFunction() = 42
fun unusedScriptFunction(p: String) = 22

<expr>scriptFunction()</expr>

fun foo(i: Int, action: (Int) -> Unit) {
    action(i)
}

foo(scriptFunction()) {
    scriptFunction()
}
