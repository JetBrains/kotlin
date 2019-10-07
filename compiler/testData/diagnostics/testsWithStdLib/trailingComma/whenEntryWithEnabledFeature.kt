// !DIAGNOSTICS: -UNUSED_VARIABLE -NAME_SHADOWING
// !LANGUAGE: +TrailingCommas

fun foo1(x: Any) = when (x) {
    Comparable::class,
    Iterable::class,
    String::class,
        -> println(1)
    else -> println(3)
}

fun foo2(x: Any) {
    val z = when (val y: Int = x as Int) {
        1, -> println(1)
        else -> println(3)
    }
}

fun foo3(x: (Any) -> Any) {
    val z = when (val y: (Any) -> Any = x) {
        {x: Any, -> x}, {y: Any, -> y}, -> println(1)
        else -> println(3)
    }
}

fun foo4(x: Any) {
    val z = when (x) {
        is Int, is Double, -> println(1)
        else -> println(3)
    }
}

fun foo5(x: Int, y: IntArray, z: IntArray) {
    val u = when (x) {
        in y,
        in z,
            -> println(1)
        else -> println(3)
    }
}

fun foo6(x: Boolean?) = when (x) {
    true, false, -> println(1)
    null, -> println(1)
}

fun foo7(x: Boolean?) = when (x) {
    true, false,/**/ -> println(1)
    null,/**/ -> println(1)
}
