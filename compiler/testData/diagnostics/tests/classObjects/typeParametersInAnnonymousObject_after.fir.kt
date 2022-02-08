// !LANGUAGE: +ProhibitTypeParametersInAnonymousObjects
// !DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-28999

fun case_1() {
    val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T><!> { } // type of x is <anonymous object><T>
}

fun case_2() {
    val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T : Number, K: Comparable<K>><!> { }
}

fun case_3() {
    val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T><!> <!SYNTAX!>where T : Comparable<T><!> { } // ERROR: Where clause is not allowed for objects
}

val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T, K: Comparable<K>><!> {
    fun test() = 10 as <!UNRESOLVED_REFERENCE!>T<!> // OK
}

fun case_4() {
    val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T><!> {
        fun test() = 10 as <!UNRESOLVED_REFERENCE!>T<!>
    }

    val y = x.test() // type y is T
}

inline fun <reified T> case_5() {
    val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T><!> {
        fun test() = 10 as T
    }

    val z = x.test()

    if (<!USELESS_IS_CHECK!>z is T<!>) {
        // z is {T!! & T!!} (smart cast from T)
        <!UNRESOLVED_REFERENCE!>println<!>(z)
    }

    val a = object<!TYPE_PARAMETERS_IN_OBJECT!><A><!> {
        fun test() = 42 as <!UNRESOLVED_REFERENCE!>A<!>
    }

    val b = a.test()

    if (a is T) {
        <!UNRESOLVED_REFERENCE!>println<!>(a)
    }
}
