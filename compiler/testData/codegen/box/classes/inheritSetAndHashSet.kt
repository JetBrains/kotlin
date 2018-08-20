// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: NATIVE

interface A : Set<String>

class B : A, HashSet<String>()

fun box(): String {
    val b = B()
    b.add("OK")
    return b.iterator().next()
}
