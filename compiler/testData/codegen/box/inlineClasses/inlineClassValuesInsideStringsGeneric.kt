// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Augmented<T: Int>(val x: T) {
    override fun toString(): String = (x + 1).toString()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsAny<T: Any>(val a: T) {
    override fun toString(): String = "AsAny: $a"
}

data class AugmentedAndAsAny(val a: Augmented<Int>, val b: AsAny<Int>)

fun box(): String {
    val a = Augmented(0)
    val single = "$a"
    if (single != "1") return "Fail 1: $single"

    val asAny = AsAny(42)
    val asAnyString = "$asAny"
    if (asAnyString != "AsAny: 42") return "Fail 2: $asAnyString"

    val b = Augmented(1)
    val two = "$a and $b"
    if (two != "1 and 2") return "Fail 3: $two"

    val d = AugmentedAndAsAny(a, asAny)
    if (d.toString() != "AugmentedAndAsAny(a=1, b=AsAny: 42)") return "Fail 4: $d"

    return "OK"
}