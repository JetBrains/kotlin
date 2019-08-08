fun test1(a: Any?) = a!!

fun test2(a: Any?) = a?.hashCode()!!

fun <X> test3(a: X) = a!!

fun useString(s: String) {}
fun <X> test4(a: X) {
    if (a is String?) a!!
    if (a is String?) useString(a!!)
}