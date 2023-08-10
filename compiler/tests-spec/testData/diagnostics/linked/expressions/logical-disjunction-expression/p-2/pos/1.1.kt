// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, logical-disjunction-expression -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: expressions, logical-disjunction-expression -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION:Both operands of a logical disjunction expression must have a type which is a subtype of kotlin.Boolean
 * HELPERS: checkType
 */

// MODULE: libModule
// FILE: libModule/JavaClass.java
package libModule;

public class JavaClass {
    public static boolean VALUE;

    public static Object VALUE_OBJ = true;

    public Boolean getValue ()
    { return new Boolean ("true"); }
}

// MODULE: mainModule(libModule)
// FILE: KotlinClass.kt
package mainModule
import libModule.*
import checkSubtype
import checkType
import check

fun foo() = run { false || JavaClass.VALUE || throw Exception() }


// TESTCASE NUMBER: 1
fun case1() {
    val a1 = false
    val a2 = JavaClass.VALUE
    val a3 = foo()
    val a4 = JavaClass().getValue()
    val a5 = JavaClass.VALUE_OBJ

    checkSubtype<Boolean>(a1)
    checkSubtype<Boolean>(a2)
    checkSubtype<Boolean>(a3)
    checkSubtype<Boolean>(a4)

    val x3 = a1 || a2 || a3 || a4 || a5 as Boolean
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>x3<!>

    x3 checkType { check<Boolean>()}
}