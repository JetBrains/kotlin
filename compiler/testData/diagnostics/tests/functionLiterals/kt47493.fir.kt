// IGNORE_LEAKED_INTERNAL_TYPES: KT-54568
fun test1() {
    try {
        { toDouble ->
        }
    } catch (e: Exception) {

    }
}

fun test2() {
    try {

    } catch (e: Exception) {
        { toDouble ->
        }
    }
}

fun box(): String {
    test1()
    test2()
    return "OK"
}
