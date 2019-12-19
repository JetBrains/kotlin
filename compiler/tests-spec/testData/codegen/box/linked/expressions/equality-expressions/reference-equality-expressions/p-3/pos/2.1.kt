// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, equality-expressions, reference-equality-expressions -> paragraph 3 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Any instance of the null reference null is equal by reference to any other instance of the null reference;
 */

// FILE: JavaClass.java
public class JavaClass{
    public static Object NULL_VALUE ;
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

    if (flag1 && flag2) return "OK"
    else
        return "NOK"
}