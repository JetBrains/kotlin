// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

class A {
    val s: Sequence<String> = sequence {
        val a = {}
        yield("OK")
    }
}

fun box(): String = A().s.single()