class TopLevelClass

fun foo(action: () -> Unit) {
    class LocalClass1

    fun localFun1(i: Int) {}
    val localVal1 = 1
}

if (true) {
    class LocalClass2
    LocalClass2()
    fun localFun2() {}
    val localVal2 = false
}

// last statement
foo {
    class LocalClass3

    fun localFun3() = true
    val localVal3 = "str"
    LocalClass3()
}
