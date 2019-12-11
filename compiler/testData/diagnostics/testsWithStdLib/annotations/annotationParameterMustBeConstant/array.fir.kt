@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Ann(val i: IntArray)

@Ann(intArrayOf(i))
@Ann(intArrayOf(i2))
@Ann(intArrayOf(i3))
@Ann(intArrayOf(i, i2, i3))
@Ann(<!INAPPLICABLE_CANDIDATE!>intArrayOf<!>(intArrayOf(i, i2, i3)))
class Test

var i = 1
const val i2 = 1
val i3 = foo()

fun foo(): Int = 1

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class AnnAnn(val i: Array<Ann>)
@AnnAnn(arrayOf(Ann(intArrayOf(1))))
@AnnAnn(arrayOf(iAnn))
class TestAnn
val iAnn = Ann(intArrayOf(1))
