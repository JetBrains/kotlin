fun test() {
    foo<Int?>()
    foo<Int>()
    bar<Int?>()
    bar<Int>()
    bar<Double?>()
    bar<Double>()
    1.<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /buzz">buzz</error><Double>()
}

fun <T : Any> foo() {}
fun <T : Int?> bar() {}
fun <T : Int> Int.buzz() : Unit {}
