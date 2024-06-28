// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Ic(val x: Int)

fun box(): String {

    val strAsAny : Any = "a"

    if ("a".equals(Ic(1))) return "Fail 1"
    if (strAsAny.equals(Ic(1))) return "Fail 2"
    if (Ic(1).equals("a")) return "Fail 3"
    if (Ic(1).equals(strAsAny)) return "Fail 4"

    return "OK"
}