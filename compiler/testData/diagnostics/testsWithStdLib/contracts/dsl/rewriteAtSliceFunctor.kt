// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// See KT-28847

class Foo(val str: String?) {
    val first = run {
        str.isNullOrEmpty()
        second
    }

    val second = str.isNullOrEmpty()
}