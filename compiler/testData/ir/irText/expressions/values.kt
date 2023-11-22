// IGNORE_BACKEND: JS_IR

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

enum class Enum { A }
object A
val a = 0
class Z {
    companion object
}

fun test1() = Enum.A
fun test2() = A
fun test3() = a
fun test4() = Z
