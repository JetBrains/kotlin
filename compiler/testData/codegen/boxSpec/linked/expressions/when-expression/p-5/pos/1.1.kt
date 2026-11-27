// WITH_STDLIB
// FULL_JDK

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 5 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The else condition must also be in the last when entry of when expression, otherwise it is a compile-time error.
 */


// FILE: JavaEnum.java
enum JavaEnum {
    Val_1,
    Val_2,
    Val_3,
}

// FILE: KotlinClass.kt
fun box(): String {
    val z = JavaEnum.Val_3
    val when1 = when (z) {
        JavaEnum.Val_1 -> { false }
        else -> {true}
    }
    val when2 = when (z) {
        JavaEnum.Val_3 -> { true }
        else -> {false}
    }
    return if (when1 && when2) "OK" else "NOK"
}