// COMPARE_WITH_LIGHT_TREE

fun List<String>.modify() {
    <!VARIABLE_EXPECTED!>this<!> += "Alpha"
    <!VARIABLE_EXPECTED!>this<!> += "Omega"
}

fun Any.modify() {
    (<!VARIABLE_EXPECTED!>this <!UNCHECKED_CAST!>as List<Int><!><!>) += 42
}

operator fun <T> Set<T>.plusAssign(x: T) {}

fun Set<String>.modify() {
    this += "Alpha"
    this += "Omega"
}

fun Any.modifySet() {
    (this <!UNCHECKED_CAST!>as Set<Int><!>) += 42
}
