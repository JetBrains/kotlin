fun test1() {
    if (true) {
        when (true) {
            true -> println()
        }
    } else {
        System.out?.println() // kotlin.Unit?
    }
}

fun test2() {
    val mlist = arrayListOf("")
    if (true) {
        when (true) {
            true -> println()
        }
    } else {
        mlist.add("") // kotlin.Boolean
    }
}