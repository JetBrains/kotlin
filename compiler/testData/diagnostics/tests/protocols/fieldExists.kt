
protocol interface ProtoInterface {
    fun foo(): Unit
    val bar: Int
}

fun printFoo(arg: ProtoInterface) {
    arg.foo()
    arg.bar
}

fun test() {
    printFoo(<!TYPE_MISMATCH!>object<!>{})

    printFoo(<!TYPE_MISMATCH!>object<!> {
        fun foo(): Unit {}
    })

    printFoo(object {
        fun foo(): Unit {}
        val bar = 42
    })
}
