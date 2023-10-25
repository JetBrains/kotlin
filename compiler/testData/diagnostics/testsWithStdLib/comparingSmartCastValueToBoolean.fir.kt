// ISSUE: KT-35134

interface A

fun foo(a: Any) {
    if (a is A) {
        if (<!EQUALITY_NOT_APPLICABLE_WARNING!>a == (<!EQUALITY_NOT_APPLICABLE_WARNING!>a == 1<!>)<!>) {
            println(1)
        }

        when (a) {
            <!CONFUSING_BRANCH_CONDITION_ERROR, EQUALITY_NOT_APPLICABLE_WARNING, INCOMPATIBLE_TYPES_WARNING!>a == 1<!> -> print("1")
        }

        if (<!EQUALITY_NOT_APPLICABLE!>(a <!USELESS_CAST!>as A<!>) == (<!EQUALITY_NOT_APPLICABLE_WARNING!>a == 1<!>)<!>) {
            println(1)
        }

        when (a <!USELESS_CAST!>as A<!>) {
            <!CONFUSING_BRANCH_CONDITION_ERROR, EQUALITY_NOT_APPLICABLE_WARNING, INCOMPATIBLE_TYPES!>a == 1<!> -> print("1")
        }
    }
}
