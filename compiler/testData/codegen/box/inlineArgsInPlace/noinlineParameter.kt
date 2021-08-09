// IGNORE_BACKEND: WASM
// WITH_RUNTIME

// TODO separate bytecode text templates for FIR?
// -- CHECK_BYTECODE_TEXT
// -- JVM_IR_TEMPLATES
// -- 1 ASTORE 1
// -- 12 ALOAD 1
// -- JVM_TEMPLATES
// -- 2 ASTORE 1
// -- 13 ALOAD 1

@Suppress("DEPRECATION_ERROR")
fun box(): String {
    val seq = buildSequence {
        yield("O")
        yield("K")
    }
    val it = seq.iterator()
    return it.next() + it.next()
}
