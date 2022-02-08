// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_BACKEND: JVM_IR

// expected: rv: <nofield>

// This example shows a bug in the old BE
// original issue - KT-49443, this is the reduced example of it.
// Here the old BE manages to compile the script, because it assumes incorrect constructor of the DefaultEachEntryConfiguration class
// on generating code for buildZip.
// The original example from the issue works supposedly because the constructor is not called on the runtime.
// In this example, uncommenting call to buildZip leads to the runtime exception, while uncommenting the call to copy or
// moving DefaultEachEntryConfiguration class definition before ZipHelper breaks codegeneration (somewhat expectedly).
// The JVM IR BE now generates correct error about invalid script instance capturing.

interface I {
    fun rename()
}

object ZipHelper {
    fun buildZip() {
        DefaultEachEntryConfiguration(0).rename()
        // 0.copy()
    }
}

class DefaultEachEntryConfiguration(val entry: Int) : I {
    override fun rename() {
        entry.copy()
    }
}

fun Int.copy() = Unit

//ZipHelper.buildZip()
