// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// Enable for JS when it supports Java class library.
// IGNORE_BACKEND: JS, NATIVE

class TestJava(r : Runnable) : Runnable by r {}
class TestRunnable() : Runnable {
  public override fun run() = System.out.println("foobar")
}

fun box() : String {
    var del = TestJava(TestRunnable())
    del.run()
    if (del !is Runnable)
        return "Fail #1"

    return "OK"
}
