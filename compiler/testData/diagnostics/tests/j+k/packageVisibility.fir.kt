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

val <!EXPOSED_PROPERTY_TYPE!>mc<!> = MyJavaClass()
val x = MyJavaClass.staticMethod()
val y = MyJavaClass.NestedClass.staticMethodOfNested()
val <!EXPOSED_PROPERTY_TYPE!>z<!> = MyJavaClass.NestedClass()

//FILE: b.kt
package b

import a.MyJavaClass

val <!EXPOSED_PROPERTY_TYPE!>mc1<!> = <!HIDDEN!>MyJavaClass<!>()

val x = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>staticMethod<!>()
val y = MyJavaClass.<!HIDDEN!>NestedClass<!>.<!HIDDEN!>staticMethodOfNested<!>()
val <!EXPOSED_PROPERTY_TYPE!>z<!> = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>NestedClass<!>()

//FILE: c.kt
package a.c

import a.MyJavaClass

val <!EXPOSED_PROPERTY_TYPE!>mc1<!> = <!HIDDEN!>MyJavaClass<!>()

val x = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>staticMethod<!>()
val y = MyJavaClass.<!HIDDEN!>NestedClass<!>.<!HIDDEN!>staticMethodOfNested<!>()
val <!EXPOSED_PROPERTY_TYPE!>z<!> = <!HIDDEN!>MyJavaClass<!>.<!HIDDEN!>NestedClass<!>()
