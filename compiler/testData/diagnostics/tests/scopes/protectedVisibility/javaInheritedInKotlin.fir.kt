// FILE: bar/JavaClass.java

package bar;

public class JavaClass {
    protected void foo() {}
    protected static void bar1() {}
    protected static void bar2() {}

    protected String field = "";
    protected static String CONST1 = "";
    protected static String CONST2 = "";

}

// FILE: foo/JavaClassSamePackage.java
package foo;

public class JavaClassSamePackage extends bar.JavaClass {
    protected static void bar2() {}
    protected static String CONST2 = "";
}

// FILE: foo/main.kt
package foo

import bar.JavaClass

class KotlinClass : JavaClass() {
    fun baz() {
        foo() // OK
    }
}

class KotlinClass2 : JavaClass() {
    override fun foo() {}

    val field: String = "abc"
}

fun test(a: KotlinClass, b: KotlinClass2) {
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>() // Error, protected_and_package declared in different package
    b.foo() // Error, protected visibility in same package (but could be protected_and_package)

    a.<!INAPPLICABLE_CANDIDATE!>field<!>

    JavaClass.<!INAPPLICABLE_CANDIDATE!>bar1<!>()
    JavaClass.<!INAPPLICABLE_CANDIDATE!>CONST1<!>

    KotlinClass.<!UNRESOLVED_REFERENCE!>bar1<!>() // Currently it's unresolved, but it should be prohibited even in case it would be resolved
    KotlinClass.<!UNRESOLVED_REFERENCE!>CONST1<!>

    JavaClassSamePackage.<!INAPPLICABLE_CANDIDATE!>bar1<!>()
    JavaClassSamePackage.bar2()

    JavaClassSamePackage.<!INAPPLICABLE_CANDIDATE!>CONST1<!>
    JavaClassSamePackage.CONST2
}
