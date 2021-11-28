// FIR_IDENTICAL
fun <T> test0(x: Any, y: T) = x is Int && x == y
fun <T : Float> test1(x: Any, y: T) = x is Float && x == y
fun <T : Double> test2(x: Any, y: T) = x is Float && x == y
fun <T : Float> test3(x: Any, y: T) = x is Int && x == y
fun <T : Float?> test4(x: Any, y: T) = x is Int && x == y
fun <T : Float?, R : T> test5(x: Any, y: R) = x is Int && x == y
fun <T : Number> test6(x: Any, y: T) = x is Int && x == y

class F<T : Float> {
    fun testCapturedType(x: T, y: Any) = y is Double && x == y
}