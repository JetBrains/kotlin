
// WITH_STDLIB
val alist = arrayListOf(1 to 2, 2 to 3, 3 to 4)

fun box(): String {
    var result = 0
    for ((i: Int, z: Int) in alist) {

        result += i + z
    }

    return if (result == 15) "OK" else "fail: $result"
}