// !LANGUAGE: +NewInference
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// FULL_JDK
// ISSUE: KT-35967

interface A {
    val s: String
}
fun test(list: List<A>) {
    sequence {
        yieldAll(list.map { it.s })
    }
}

fun box(): String = "OK"
