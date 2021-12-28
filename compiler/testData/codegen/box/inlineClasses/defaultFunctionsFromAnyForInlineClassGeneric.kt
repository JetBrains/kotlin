// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class FooRef<T: String>(val y: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class FooLong<T: Long>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class FooDouble<T: Double>(val y: T)

fun box(): String {
    val f = Foo(42)
    if (f.toString() != "Foo(x=42)") return "Fail 1: $f"

    if (!f.equals(f)) return "Fail 2"

    val g = Foo(43)
    if (f.equals(g)) return "Fail 3"

    if (42.hashCode() != f.hashCode()) return "Fail 4"

    val fRef = FooRef("42")
    if (fRef.toString() != "FooRef(y=42)") return "Fail 5: $fRef"

    if (!fRef.equals(fRef)) return "Fail 6"

    val gRef = FooRef("43")
    if (fRef.equals(gRef)) return "Fail 7"

    if ("42".hashCode() != fRef.hashCode()) return "Fail 8"

    val fLong = FooLong(42)
    if (fLong.toString() != "FooLong(x=42)") return "Fail 9: $fLong"

    if (!fLong.equals(fLong)) return "Fail 10"

    val gLong = FooLong(43)
    if (fLong.equals(gLong)) return "Fail 11"

    if (42L.hashCode() != fLong.hashCode()) return "Fail 12"

    val fDouble = FooDouble(42.1)
    if (fDouble.toString() != "FooDouble(y=42.1)") return "Fail 13: $fDouble"

    if (!fDouble.equals(fDouble)) return "Fail 14"

    val gDouble = FooDouble(43.0)
    if (fDouble.equals(gDouble)) return "Fail 15"

    if (42.1.hashCode() != fDouble.hashCode()) return "Fail 16"

    return "OK"
}