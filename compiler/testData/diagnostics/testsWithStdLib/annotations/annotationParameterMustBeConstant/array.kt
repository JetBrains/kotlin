annotation class Ann(val i: IntArray)

Ann(intArray(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>i<!>))
Ann(intArray(i2))
Ann(intArray(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>i3<!>))
Ann(intArray(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>i<!>, i2, <!ANNOTATION_PARAMETER_MUST_BE_CONST!>i3<!>))
Ann(intArray(<!TYPE_MISMATCH!>intArray(i, i2, i3)<!>))
class Test

var i = 1
val i2 = 1
val i3 = foo()

fun foo(): Int = 1

annotation class AnnJC(val i: Array<Class<*>>)
AnnJC(array(javaClass<Test>()))
AnnJC(array(<!ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL!>iJC<!>))
class TestJC
val iJC = javaClass<Test>()

annotation class AnnAnn(val i: Array<Ann>)
AnnAnn(array(Ann(intArray(1))))
AnnAnn(array(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>iAnn<!>))
class TestAnn
val iAnn = <!ANNOTATION_CLASS_CONSTRUCTOR_CALL!>Ann(intArray(1))<!>