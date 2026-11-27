// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, equality-expressions, reference-equality-expressions -> paragraph 3 -> sentence 2
 * NUMBER: 1
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

    var flag1 = false
    if (null === JavaClass.NULL_VALUE) {
        if (JavaClass.NULL_VALUE === null)
            flag1 = true
    }

    var flag2 = false
    val x = null
    if (null === x) {
        if (x === null)
            flag2 = true
    }

    var flag3 = false
    if (null === null)
        flag3 = true

    var flag4 = false
    val s: String? = null
    if (s === JavaClass.NULL_VALUE)
        if (JavaClass.NULL_VALUE === s)
            flag4 = true

    var flag5 = false
    if (null === JavaClass().x)
        if (JavaClass().x === null)
            flag5 = true

    var flag6 = false
    if (null === JavaClass.id(null))
        if (JavaClass.id(null) === null)
            flag6 = true


    if (flag1 && flag2 && flag3 && flag4 && flag5 &&flag6) return "OK"
    else
        return "NOK"
}