@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Ann(val i: Int)
annotation class AnnIA(val ia: IntArray)
annotation class AnnSA(val sa: Array<String>)

@Ann(MyClass().i)
@Ann(i)
@Ann(i2)
@AnnIA(ia)
@AnnSA(sa)
class Test {
    val i = 1
    @Ann(i) val i2 = 1
}

var i = 1
val i2 = foo()

fun foo(): Int = 1

@AnnSA(emptyArray())
class MyClass {
    val i = 1
}

val ia: IntArray = intArrayOf(1, 2)
val sa: Array<String> = arrayOf("a", "b")

annotation class Ann2
