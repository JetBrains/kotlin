annotation(repeatable = true, retention = AnnotationRetention.SOURCE) class Ann(val i: Int)
annotation class AnnIA(val ia: IntArray)
annotation class AnnSA(val sa: Array<String>)

Ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>MyClass().i<!>)
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

val ia: IntArray = intArrayOf(1, 2)
val sa: Array<String> = arrayOf("a", "b")

annotation class Ann2
