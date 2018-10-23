// FIX: Replace total order equality with IEEE 754 equality

fun <T> test(a: T, b: T) =
    a is Double && b is Double && a.<caret>equals(b)