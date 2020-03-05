// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
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
    public static final Boolean aB;
    public static Object aO = false;
}

// MODULE: mainModule(libModule)
// FILE: KotlinClass.kt
package mainModule
import libModule.*
import checkSubtype

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35517
 */
fun case1() {
    val a: Any = true
    if (<!TYPE_MISMATCH, TYPE_MISMATCH!>a<!>) { "true" } else "false"
    checkSubtype<Boolean>(<!TYPE_MISMATCH!>a<!>)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35517
 */
fun case2() {
    val a = JavaContainer.aO
    if (<!TYPE_MISMATCH, TYPE_MISMATCH!>a<!>) { "true" } else "false"
    checkSubtype<Boolean>(<!TYPE_MISMATCH!>a<!>)
}
