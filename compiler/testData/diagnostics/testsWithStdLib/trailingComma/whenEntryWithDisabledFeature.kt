// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_EXPRESSION -NAME_SHADOWING

fun foo1(x: Any) = when (x) {
    Comparable::class,
    Iterable::class,
    String::class<!UNSUPPORTED_FEATURE!>,<!>
        -> println(1)
    else -> println(3)
}

fun foo2(x: Any) {
    val z = when (val y: Int = x as Int) {
        1<!UNSUPPORTED_FEATURE!>,<!> -> println(1)
        else -> println(3)
    }
}

fun foo3(x: (Any) -> Any) {
    val z = when (val y: (Any) -> Any = x) {
        {x: Any<!UNSUPPORTED_FEATURE!>,<!> -> x}, {y: Any<!UNSUPPORTED_FEATURE!>,<!> -> y}<!UNSUPPORTED_FEATURE!>,<!> -> println(1)
        else -> println(3)
    }
}

fun foo4(x: Any) {
    val z = when (x) {
        is Int, is Double<!UNSUPPORTED_FEATURE!>,<!> -> println(1)
        else -> println(3)
    }
}

fun foo5(x: Int, y: IntArray, z: IntArray) {
    val u = when (x) {
        in y,
        in z<!UNSUPPORTED_FEATURE!>,<!>
            -> println(1)
        else -> println(3)
    }
}

fun foo6(x: Boolean?) = when (x) {
    true, false<!UNSUPPORTED_FEATURE!>,<!> -> println(1)
    null<!UNSUPPORTED_FEATURE!>,<!> -> println(1)
}

fun foo7(x: Boolean?) = when (x) {
    true, false<!UNSUPPORTED_FEATURE!>,<!>/**/ -> println(1)
    null<!UNSUPPORTED_FEATURE!>,<!>/**/ -> println(1)
}
