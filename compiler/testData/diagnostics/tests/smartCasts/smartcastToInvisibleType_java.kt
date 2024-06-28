// ISSUES: KT-44802, KT-56744
// INFERENCE_HELPERS

// FILE: foo/PackagePrivateInterface.java
package foo;

interface PackagePrivateInterface {
    default void foo() {}
}

// FILE: foo/A.java
package foo;

public class A implements PackagePrivateInterface {}

// FILE: foo/B.java
package foo;

public class B implements PackagePrivateInterface {}

// FILE: differentPackage.kt
package bar

import foo.A
import foo.B
import select

fun testSmartcast(x: Any) {
    if (x is A || x is B) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun testInference(a: A, b: B) {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("foo.PackagePrivateInterface"), INACCESSIBLE_TYPE!>select(a, b)<!>
    <!INACCESSIBLE_TYPE!>x<!>.<!INVISIBLE_MEMBER!>foo<!>()
}

// FILE: samePackage.kt
package foo

import select

fun testSmartcast(x: Any) {
    if (x is A || x is B) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun testInference(a: A, b: B) {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("foo.PackagePrivateInterface")!>select(a, b)<!>
    x.foo()
}
