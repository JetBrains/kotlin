@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Ann(val i: Int)
annotation class AnnIA(val ia: IntArray)
annotation class AnnSA(val sa: Array<String>)

var i = 1

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyClass().i<!>)
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i<!>)
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i2<!>)
@AnnIA(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>ia<!>)
@AnnSA(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>sa<!>)
class Test {
    val i = 1
    @Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i<!>) val i2 = 1
}

val i2 = foo()

fun foo(): Int = 1

@AnnSA(emptyArray())
class MyClass {
    val i = 1
}

val ia: IntArray = intArrayOf(1, 2)
val sa: Array<String> = arrayOf("a", "b")

annotation class Ann2
