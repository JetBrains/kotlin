val ok = "OK"
val ok2 = ok
val ok3: String get() = "OK"

fun test1() = ok

fun test2(x: String) = x

fun test3(): String {
    val x = "OK"
    return x
}

fun test4() = ok3

val String.okext: String get() = "OK"
fun String.test5() = okext
