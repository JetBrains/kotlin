//FILE: a/MyJavaClass.java
package a;

class MyJavaClass {
    static int staticMethod() {
        return 1;
    }

    static class NestedClass {
        static int staticMethodOfNested() {
            return 1;
        }
    }
}

//FILE:a.kt
package a

val mc = MyJavaClass()
val x = MyJavaClass.staticMethod()
val y = MyJavaClass.NestedClass.staticMethodOfNested()
val z = MyJavaClass.NestedClass()

//FILE: b.kt
package b

import a.MyJavaClass

val mc1 = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>()

val x = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>.<!UNRESOLVED_REFERENCE!>staticMethod<!>()
val y = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>.<!UNRESOLVED_REFERENCE!>NestedClass<!>.<!UNRESOLVED_REFERENCE!>staticMethodOfNested<!>()
val z = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>.<!UNRESOLVED_REFERENCE!>NestedClass<!>()

//FILE: c.kt
package a.c

import a.MyJavaClass

val mc1 = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>()

val x = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>.<!UNRESOLVED_REFERENCE!>staticMethod<!>()
val y = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>.<!UNRESOLVED_REFERENCE!>NestedClass<!>.<!UNRESOLVED_REFERENCE!>staticMethodOfNested<!>()
val z = <!INAPPLICABLE_CANDIDATE!>MyJavaClass<!>.<!UNRESOLVED_REFERENCE!>NestedClass<!>()