class Host {
    operator fun plusAssign(x: Int) {}

    fun test1() {
        this += 1
    }
}

fun foo() = Host()

fun Host.test2() {
    this += 1
}

fun test3() {
    foo() += 1
}

fun test4(a: () -> Host) {
    a() += 1
}
