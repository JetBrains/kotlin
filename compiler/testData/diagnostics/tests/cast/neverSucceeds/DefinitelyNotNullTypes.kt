// FIR_IDENTICAL

fun <T, K> test(x: T & Any) {
    x <!USELESS_CAST!>as (T & Any)<!>
    x <!UNCHECKED_CAST!>as (K & Any)<!>
}
