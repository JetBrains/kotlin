annotation class Ann(val i: Int)
annotation class AnnIA(val ia: IntArray)
annotation class AnnSA(val sa: Array<String>)

Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>MyClass().i<!>)
Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>O.i<!>)
Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>i<!>)
Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>i2<!>)
AnnIA(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>ia<!>)
AnnSA(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>sa<!>)
class Test {
    val i = 1
    Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>i<!>) val i2 = 1
}

var i = 1
val i2 = foo()

fun foo(): Int = 1

class MyClass {
    val i = 1
}

object O {
    val i = 1
}

val ia: IntArray = intArray(1, 2)
val sa: Array<String> = array("a", "b")

annotation class Ann2

// from stdlib
fun <T> array(vararg t : T) : Array<T> = t
fun intArray(vararg content : Int) : IntArray = content
