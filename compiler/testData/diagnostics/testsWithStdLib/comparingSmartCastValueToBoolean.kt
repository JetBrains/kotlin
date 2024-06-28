// ISSUE: KT-35134

interface A

fun foo(a: Any) {
    if (a is A) {
        if (a == (a == 1)) {
            println(1)
        }

        when (a) {
            <!CONFUSING_BRANCH_CONDITION_ERROR!>a == 1<!> -> print("1")
        }

        if (<!EQUALITY_NOT_APPLICABLE!>(a <!USELESS_CAST!>as A<!>) == (a == 1)<!>) {
            println(1)
        }

        when (a <!USELESS_CAST!>as A<!>) {
            <!CONFUSING_BRANCH_CONDITION_ERROR, INCOMPATIBLE_TYPES!>a == 1<!> -> print("1")
        }
    }
}
