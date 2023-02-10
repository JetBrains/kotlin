// ISSUE: KT-53752
// INFERENCE_HELPERS

interface A
interface B {
    fun fooB(x: Int): String
}

class Foo

fun test(ab: A) {
    if (ab is B) {
        var z = id(ab) // materialize smartcast
        z = <!ASSIGNMENT_TYPE_MISMATCH!>Foo()<!> // unsafe assignment
        z.fooB(1)
    }
}
