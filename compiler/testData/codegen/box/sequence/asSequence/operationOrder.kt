// WITH_STDLIB

class Test() {
    private val list = listOf(1, true, 2, true, 3, true, 4, true)
//  bad list:   private val list = listOf(1, 2, 3, 4, true, true, true, true)
    private var index = 0
    fun test(value: Int): Boolean = value == list[index++]
    fun test(value: Boolean): Boolean = value == list[index++]
}

class Test2() {
    fun test(value: Int): Boolean = value == counter++
    companion object {
        var counter = 1
    }
}

fun box(): String {
    val list = listOf(1, 2, 3, 4)
    val seq = list.asSequence()
    val test = Test()
    for (item in seq.map { test.test(it) }.map { test.test(it) }) {
        if (!item) return "Fail on seq"
    }
    val list2 = listOf(Test2() to 1, Test2() to 3)
    val seq2 = list2.asSequence()
    for (item in seq2.map {
        val test = it.first
        val i = it.second
        if(test.test(i)) test to i + 1 else test to 100
    }.map {
        val test = it.first
        val i = it.second
        test.test(i)
    }) {
        if (!item) return "Fail on seq2"
    }
    return "OK"
}