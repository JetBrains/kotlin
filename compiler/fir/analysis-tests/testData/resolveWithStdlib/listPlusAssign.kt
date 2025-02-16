// RUN_PIPELINE_TILL: FRONTEND
// COMPARE_WITH_LIGHT_TREE

fun List<String>.modify() {
    <!VARIABLE_EXPECTED!>this<!> += "Alpha"
    <!VARIABLE_EXPECTED!>this<!> += "Omega"
}

fun Any.modify() {
    <!CANNOT_INFER_PARAMETER_TYPE!>(this <!UNCHECKED_CAST!>as List<Int><!>) <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+=<!> 42<!>
}

operator fun <T> Set<T>.plusAssign(x: T) {}

fun Set<String>.modify() {
    this += "Alpha"
    this += "Omega"
}

fun Any.modifySet() {
    (this <!UNCHECKED_CAST!>as Set<Int><!>) += 42
}
