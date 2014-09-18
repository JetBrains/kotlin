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

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

val mc1 = <!INVISIBLE_MEMBER!>MyJavaClass<!>()

val x = MyJavaClass.<!INVISIBLE_MEMBER!>staticMethod<!>()
val y = MyJavaClass.NestedClass.<!INVISIBLE_MEMBER!>staticMethodOfNested<!>()
val z = MyJavaClass.<!INVISIBLE_MEMBER!>NestedClass<!>()

//FILE: c.kt
package a.c

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

val mc1 = <!INVISIBLE_MEMBER!>MyJavaClass<!>()

val x = MyJavaClass.<!INVISIBLE_MEMBER!>staticMethod<!>()
val y = MyJavaClass.NestedClass.<!INVISIBLE_MEMBER!>staticMethodOfNested<!>()
val z = MyJavaClass.<!INVISIBLE_MEMBER!>NestedClass<!>()
