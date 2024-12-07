// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN

fun GuardMustHaveExpression(x: Any) {
    when (x) {
        is Boolean if<!SYNTAX!><!> -> Unit
        is Boolean if<!SYNTAX!><!>
        -> Unit

        is Boolean
            if<!SYNTAX!><!> -> Unit
        is Boolean
            if<!SYNTAX!><!>
        -> Unit

        else if<!SYNTAX!><!> -> Unit
        else if<!SYNTAX!><!>
        -> Unit

        else
            if<!SYNTAX!><!> -> Unit
        else
            if<!SYNTAX!><!>
        -> Unit
    }
}

fun GuardNewlines(x: Any) {
    when (x) {
        // Test new-lines in guarded when conditions (without parentheses)
        is Boolean if
            x == true -> Unit
        is Boolean if
            x == true
        -> Unit
        is Boolean if x == true ||
            x == false -> Unit
        is Boolean
            if x == true -> Unit
        is Boolean
            if x == true
        -> Unit
        is Boolean
            if
                x == true -> Unit
        is Boolean
            if
                x == true
        -> Unit

        // Test new-lines in guarded when conditions (with parentheses)
        is Boolean if
            (x == true) -> Unit
        is Boolean if
            (x == true)
        -> Unit
        is Boolean if (x == true ||
            x == false) -> Unit
        is Boolean
            if (x == true) -> Unit
        is Boolean
            if (x == true)
        -> Unit
        is Boolean
            if
                (x == true) -> Unit
        is Boolean
            if
                (x == true)
        -> Unit

        // Test new-lines in guarded else (without parentheses)
        else if
            x == true -> Unit
        else if
            x == true
        -> Unit
        else if x == true ||
            x == false -> Unit
        else
            if x == true -> Unit
        else
            if x == true
        -> Unit
        else
            if
                x == true -> Unit
        else
            if
                x == true
        -> Unit

        // Test new-lines in guarded else (with parentheses)
        else if
            (x == true) -> Unit
        else if
            (x == true)
        -> Unit
        else if (x == true ||
            x == false) -> Unit
        else
            if (x == true) -> Unit
        else
            if (x == true)
        -> Unit
        else
            if
                (x == true) -> Unit
        else
            if
                (x == true)
        -> Unit
    }
}

fun IfExpressionNotGuard(x: Any) {
    when (x) {
        if (x is Boolean) true else false,
        if (x is String) true else false,
        -> Unit
    }
}
