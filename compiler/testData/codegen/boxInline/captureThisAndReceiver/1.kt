fun test1() : Int {
    val inlineX = My(111)

    return inlineX.perform<My, Int>{

        val outX = My(1111111)
        outX.perform<My, Int>(
                {inlineX.value}
        )
    }
}

inline fun My.execute(): Int {
    return perform { this.value }
}

fun test2(): Int {
    val inlineX = My(11)

    return inlineX.execute()
}

fun box(): String {
    if (test1() != 111) return "test1: ${test1()}"
    if (test2() != 11) return "test2: ${test2()}"

    return "OK"
}