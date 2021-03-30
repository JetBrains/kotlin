fun test() {
    foo<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'kotlin/Any'">Int?</error>>()
    foo<Int>()
    bar<Int?>()
    bar<Int>()
    bar<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'kotlin/Int?'">Double?</error>>()
    bar<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'kotlin/Int?'">Double</error>>()
    1.<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /buzz">buzz</error><Double>()
}

fun <T : Any> foo() {}
fun <T : Int?> bar() {}
fun <T : Int> Int.buzz() : Unit {}
