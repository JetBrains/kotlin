// FIX: Replace total order equality with IEEE 754 equality

fun test(a: Any, b: Any) =
    a is Double && b is Double && a.<caret>equals(b)