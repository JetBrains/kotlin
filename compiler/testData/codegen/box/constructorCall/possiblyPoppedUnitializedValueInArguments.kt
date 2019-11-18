// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
fun box(): String {
    for (count in 0..3) {
        val test = Foo(count, Foo(1, "x", if (count > 0) break else 2), 3)
        if (count > 0) return "Fail: count = $count"
        if (test.toString() != "Foo(0,Foo(1,x,2),3)") return "Fail: ${test.toString()}"
    }

    return "OK"
}


// FILE: util.kt
val log = StringBuilder()

fun <T> logged(msg: String, value: T): T {
    log.append(msg)
    return value
}

// FILE: Foo.kt
class Foo(val a: Int, val b: Any, val c: Int) {
    init {
        log.append("<init>")
    }

    override fun toString() = "Foo($a,$b,$c)"

    companion object {
        init {
            log.append("<clinit>")
        }
    }
}