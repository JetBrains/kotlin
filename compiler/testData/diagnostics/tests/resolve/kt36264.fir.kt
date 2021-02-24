// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A
class B : A

val String.ext: A
    get() = TODO()

class Cls {
    fun take(arg: B) {}

    fun test(s: String) {
        if (s.ext is B)
            take(s.ext)
    }
}

fun take(arg: Any) {}
