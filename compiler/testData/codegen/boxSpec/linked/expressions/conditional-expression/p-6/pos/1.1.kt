// FULL_JDK
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, conditional-expression -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The type of the condition expression must be a subtype of kotlin.Boolean, otherwise it is an error
 */

// FILE: JavaClass.java
public class JavaClass{
    public Boolean x;
}

// FILE: KotlinClass.kt
import java.lang.IllegalStateException

fun box(): String {
    return try {
        val a = if (JavaClass().x) { "NOK" } else "NOK"
        a
    } catch (e: java.lang.NullPointerException) {
        "OK"
    }
}