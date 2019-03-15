// FIX: Replace total order equality with IEEE 754 equality

fun test(a: Any, b: Any) =
    a is Int && b is Double && a.<caret>equals(b)