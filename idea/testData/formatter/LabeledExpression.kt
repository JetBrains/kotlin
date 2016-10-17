fun test1() {
    loop@    while(true) {

    }
}

fun test2() {
    listOf(1).forEach lit@    {
        if (it == 0) return@lit
        print(it)
    }
}