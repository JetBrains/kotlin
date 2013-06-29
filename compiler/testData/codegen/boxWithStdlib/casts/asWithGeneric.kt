fun test1<T>() = null as T
fun test2<T>(): T {
   val a : Any? = null
   return a as T
}

fun test3<T: Any>() = null as T

fun box(): String {
    if (test1<Int?>() != null) return "fail: test1"
    if (test2<Int?>() != null) return "fail: test2"
    var result3 = "fail"
    try {
        test3<Int>()
    }
    catch(e: TypeCastException) {
        result3 = "OK"
    }
    if (result3 != "OK") return "fail: test3"
    return "OK"
}
