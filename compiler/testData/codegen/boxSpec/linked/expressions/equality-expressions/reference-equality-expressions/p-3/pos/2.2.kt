// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, equality-expressions, reference-equality-expressions -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Any instance of the null reference null is equal by reference to any other instance of the null reference;
 */

// FILE: JavaClass.java
public class JavaClass {
    public static Object NULL_VALUE;

    public Integer x;

    public static <T> T id(T x) { return null; }
}

// FILE: KotlinClass.kt
fun box(): String {
    if (null !== JavaClass.NULL_VALUE) return "NOK"
    if (JavaClass.NULL_VALUE !== null) return "NOK"

    val x = null
    if (null !== x) return "NOK"
    if (x !== null) return "NOK"

    if (null !== null) return "NOK"

    val s: String? = null
    if (s !== JavaClass.NULL_VALUE) return "NOK"
    if (JavaClass.NULL_VALUE !== s) return "NOK"

    if (getNull(true) !== getNull(false) === getNull(true) !== getNull(false) === getNull(true)) return "NOK"

    if (JavaClass().x !== null) return "NOK"
    if (null !== JavaClass().x ) return "NOK"

    if (JavaClass.id(null) !== null) return "NOK"
    if (null !== JavaClass.id(null) ) return "NOK"

    return "OK"
}

fun getNull(x: Boolean): Any? = if (x) null else ""
