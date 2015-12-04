import test.*

fun case1(): Int =
        null.castTo<Int?, Int>()

fun box(): String {
    failTypeCast { case1(); return "failTypeCast 9" }
    return "OK"
}

inline fun failTypeCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: TypeCastException) {

    }
}