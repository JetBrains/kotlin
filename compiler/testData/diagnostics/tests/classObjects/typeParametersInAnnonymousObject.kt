// !DIAGNOSTICS: -UNUSED_VARIABLE!
// ISSUE: KT-28999

fun case_1() {
    val x = object<!TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT!><T><!> { } // type of x is <anonymous object><T>
}

fun case_2() {
    val x = object<!TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT!><T : Number, K: Comparable<K>><!> { }
}

fun case_3() {
    val x = object<!TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT!><T><!> <!SYNTAX!>where <!DEBUG_INFO_MISSING_UNRESOLVED!>T<!> : <!DEBUG_INFO_MISSING_UNRESOLVED!>Comparable<!><<!DEBUG_INFO_MISSING_UNRESOLVED!>T<!>><!> { } // ERROR: Where clause is not allowed for objects
}

val x = object<!TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT!><T, K: Comparable<K>><!> {
    fun test() = 10 <!UNCHECKED_CAST!>as T<!> // OK
}

fun case_4() {
    val x = object<!TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT!><T><!> {
        fun test() = 10 <!UNCHECKED_CAST!>as T<!>
    }

    val y = x.test() // type y is T
}

inline fun <reified T> case_5() {
    val x = object<!TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT!><T><!> {
        fun test() = 10 <!UNCHECKED_CAST!>as T<!>
    }

    val z = x.test()

    if (z is T) {
        // z is {T!! & T!!} (smart cast from T)
        <!UNRESOLVED_REFERENCE!>println<!>(z)
    }
}