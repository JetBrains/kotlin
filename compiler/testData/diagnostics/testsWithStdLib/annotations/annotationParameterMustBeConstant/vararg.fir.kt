@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Ann(vararg val i: Int)

@Ann(i)
@Ann(i2)
@Ann(i3)
@Ann(i, i2, i3)
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
