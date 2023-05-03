// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

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
    if (<!CONDITION_TYPE_MISMATCH!>a<!>) { "true" } else "false"
    checkSubtype<Boolean>(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-35517
 */
fun case2() {
    val a = JavaContainer.aO
    if (<!CONDITION_TYPE_MISMATCH!>a<!>) { "true" } else "false"
    checkSubtype<Boolean>(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}

// TESTCASE NUMBER: 3
// FILE: JavaClassCase3.java
public class JavaClassCase3{
    public static <T> T id(T x) {
        return null;
    }
}

// FILE: KotlinClassCase3.kt
// TESTCASE NUMBER: 3
fun case3() {
    val x = JavaClassCase3.id(null) // Nothing!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing..kotlin.Nothing?!")!>x<!>
    val a = if (x) {
        "NOK"
    } else "NOK"
}
