// COMPARE_WITH_LIGHT_TREE

fun List<String>.modify() {
    <!VARIABLE_EXPECTED{PSI}!>this<!> += "Alpha"
    <!VARIABLE_EXPECTED{PSI}!>this<!> += "Omega"
}

fun Any.modify() {
    (<!VARIABLE_EXPECTED!>this as List<Int><!>) += 42
}

operator fun <T> Set<T>.plusAssign(x: T) {}

fun Set<String>.modify() {
    this += "Alpha"
    this += "Omega"
}

fun Any.modifySet() {
    (this as Set<Int>) += 42
}
