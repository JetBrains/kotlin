// WITH_STDLIB
// FULL_JDK

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: it is possible to  replace the else condition with an always-true condition (Enum)
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
    val when3 = when (z) {
        JavaEnum.Val_1 -> { "NOK" }
        JavaEnum.Val_3 -> { "OK" }
        JavaEnum.Val_3 -> { "NOK" }
        JavaEnum.Val_2 -> { "NOK" }
    }
    return when3
}