// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, conditional-expression -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: The type of the condition expression must be a subtype of kotlin.Boolean
 * HELPERS: checkType
 */

// MODULE: libModule
// FILE: libModule/JavaContainer.java
package libModule;

public class JavaContainer {
    public static boolean ab;
    public static Boolean aB;
    public static final Object aO = false;
}

// MODULE: mainModule(libModule)
// FILE: KotlinClass.kt
package mainModule
import libModule.*
import checkSubtype
// TESTCASE NUMBER: 1
fun case1() {
    val a = JavaContainer.ab
    if (a) { "true" } else "false"
    checkSubtype<Boolean>(a)
    checkSubtype<Boolean>(false)
}

// TESTCASE NUMBER: 2
fun case2() {
    val a = JavaContainer.aB
    if (a) { "true" } else "false"
    checkSubtype<Boolean>(a)
}

// TESTCASE NUMBER: 3
fun case3() {
    val a = JavaContainer.aO as Boolean
    if (a) { "true" } else "false"
    checkSubtype<Boolean>(a)
}

// TESTCASE NUMBER: 4
fun case4(a: Nothing) {
    checkSubtype<Boolean>(a)
    if (a) { "true" } else "false"

}