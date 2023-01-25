fun <T> bar(): T {
    return null <!UNCHECKED_CAST!>as T<!>
}

class X() : <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>B<!> by <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!VARIABLE_EXPECTED!>get()<!> = bar()<!> {
    val prop = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!VARIABLE_EXPECTED!>bar()<!> = 2<!>
}

