// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun test(x: Int?) {
    val a1 = x <!USELESS_CAST!>as? Int<!>
    val a2 = x <!USELESS_CAST!>as? Int?<!>
    val a3 = x as? Number
    val a4 = x as? Number?
    val a5: Int? = x <!USELESS_CAST!>as? Int<!>
    val a6: Number? = x <!USELESS_CAST!>as? Int<!>
    val a7: Number? = 1 <!USELESS_CAST!>as? Number<!>

    run { x <!USELESS_CAST!>as? Int<!> }
    run { x <!NI;USELESS_CAST!>as? Number<!> }

    foo(x as? Number)

    if (x is Int) {
        val b = x as? Int
    }
}

fun foo(x: Number?) {}