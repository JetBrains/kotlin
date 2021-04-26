// IGNORE_BACKEND: WASM
// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 Function(^.)*.invoke

class C(val x: String) {
    fun foo(s: String) = x + s
}

fun box() =
    C("O")::foo.invoke("K")
