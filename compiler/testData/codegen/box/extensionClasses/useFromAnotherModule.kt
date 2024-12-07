// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// FIR status: context receivers aren't yet supported

// MODULE: lib
// FILE: A.kt

package a

class O(val o: String)

context(O)
class OK(val k: String) {
    val result = o + k
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return with(a.O("O")) {
        val ok = a.OK("K")
        ok.result
    }
}
