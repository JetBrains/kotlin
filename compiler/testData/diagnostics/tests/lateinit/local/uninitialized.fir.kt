// !LANGUAGE: +LateinitLocalVariables

fun test1() {
    lateinit var s: String
    <!UNINITIALIZED_VARIABLE!>s<!>.length
}

fun test2() {
    lateinit var s: String
    run {
        s = ""
    }
    s.length
}

fun almostAlwaysTrue(): Boolean = true

fun test3() {
    lateinit var s: String
    if (almostAlwaysTrue()) {
        s = ""
    }
    <!UNINITIALIZED_VARIABLE!>s<!>.length
}