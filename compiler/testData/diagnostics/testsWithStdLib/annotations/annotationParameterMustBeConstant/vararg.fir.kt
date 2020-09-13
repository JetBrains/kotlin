@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Ann(vararg val i: Int)

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i<!>)
@Ann(i2)
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i3<!>)
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i<!>, i2, <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>i3<!>)
@Ann(*intArrayOf(i))
@Ann(*intArrayOf(i2))
@Ann(*intArrayOf(i3))
@Ann(*intArrayOf(i, i2, i3))
class Test

var i = 1
const val i2 = 1
val i3 = foo()

fun foo(): Int = 1

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class AnnAnn(vararg val i: Ann)
@AnnAnn(*arrayOf(Ann(1)))
@AnnAnn(*arrayOf(iAnn))
class TestAnn
val iAnn = Ann(1)
