val flag = true

val a: () -> Int = l@ {
    <!TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (flag) return@l 4<!>
}

val b: () -> Unit = l@ {
    if (flag) return@l <!CONSTANT_EXPECTED_TYPE_MISMATCH!>4<!>
}

val c: () -> Any = l@ {
    <!INVALID_IF_AS_EXPRESSION!>if<!> (flag) return@l 4
}

val d: () -> Int = l@ {
    if (flag) return@l 4
    5
}

val e: () -> Int = l@ {
    <!TYPE_MISMATCH!>if (flag) <!UNUSED_EXPRESSION!>4<!><!>
}