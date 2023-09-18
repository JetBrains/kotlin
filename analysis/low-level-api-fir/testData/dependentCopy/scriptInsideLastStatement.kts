class ScriptClass {
    val memberProperty = 4
}

fun scriptFunction() = 42
fun unusedScriptFunction(p: String) = 22

scriptFunction()

fun foo(i: Int, action: (Int) -> Unit) {
    action(i)
}

foo(scriptFunction()) {
    scri<caret>ptFunction()
}
