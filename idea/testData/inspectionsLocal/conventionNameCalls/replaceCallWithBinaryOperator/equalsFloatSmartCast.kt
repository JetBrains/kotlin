// FIX: Replace total order equality with IEEE 754 equality

fun test(a: Any, b: Any) =
    a is Float && b is Float && a.<caret>equals(b)