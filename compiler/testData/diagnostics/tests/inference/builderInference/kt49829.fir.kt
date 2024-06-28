// WITH_STDLIB
// ISSUE: KT-50293

fun main() {
    val list = buildList {
        add("one")
        add("two")

        val secondParameter = get(1)
        println(secondParameter <!USELESS_CAST!>as String<!>)
    }
}