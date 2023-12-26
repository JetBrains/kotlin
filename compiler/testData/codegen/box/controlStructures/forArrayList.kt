// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

val alist = arrayListOf(1, 2, 3) // : j.u.ArrayList<k.Int>

fun box(): String {
    var result = 0
    for (i: Int in alist) {
        result += i
    }

    return if (result == 6) "OK" else "fail: $result"
}