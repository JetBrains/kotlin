// WITH_RUNTIME
fun test(b: Boolean) {
    val list1 = mutableListOf(1)
    val list2 = mutableListOf(2)

    when {
        b -> list1
        else -> <caret>{
            list2
        }
    }.add(3)
}