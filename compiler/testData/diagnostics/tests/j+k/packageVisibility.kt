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

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

val <!EXPOSED_PROPERTY_TYPE!>mc1<!> = <!INVISIBLE_MEMBER!>MyJavaClass<!>()

val x = <!INVISIBLE_REFERENCE!>MyJavaClass<!>.<!INVISIBLE_MEMBER!>staticMethod<!>()
val y = <!INVISIBLE_REFERENCE!>MyJavaClass<!>.<!INVISIBLE_REFERENCE!>NestedClass<!>.<!INVISIBLE_MEMBER!>staticMethodOfNested<!>()
val <!EXPOSED_PROPERTY_TYPE!>z<!> = <!INVISIBLE_REFERENCE!>MyJavaClass<!>.<!INVISIBLE_MEMBER!>NestedClass<!>()

//FILE: c.kt
package a.c

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

val <!EXPOSED_PROPERTY_TYPE!>mc1<!> = <!INVISIBLE_MEMBER!>MyJavaClass<!>()

val x = <!INVISIBLE_REFERENCE!>MyJavaClass<!>.<!INVISIBLE_MEMBER!>staticMethod<!>()
val y = <!INVISIBLE_REFERENCE!>MyJavaClass<!>.<!INVISIBLE_REFERENCE!>NestedClass<!>.<!INVISIBLE_MEMBER!>staticMethodOfNested<!>()
val <!EXPOSED_PROPERTY_TYPE!>z<!> = <!INVISIBLE_REFERENCE!>MyJavaClass<!>.<!INVISIBLE_MEMBER!>NestedClass<!>()