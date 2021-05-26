// FIR_IDENTICAL
// !LANGUAGE: +ExpectedTypeFromCast

package pp

class A {
    fun <T> foo(): T = TODO()

    companion object {
        fun <T> foo2(): T = TODO()
    }
}

val x = A().<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as String
val y = A.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo2<!>() as String
val z = pp.A.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo2<!>() as String
