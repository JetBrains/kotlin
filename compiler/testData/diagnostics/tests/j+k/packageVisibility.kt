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

<!EXPOSED_PROPERTY_TYPE!>val mc = MyJavaClass()<!>
val x = MyJavaClass.staticMethod()
val y = MyJavaClass.NestedClass.staticMethodOfNested()
<!EXPOSED_PROPERTY_TYPE!>val z = MyJavaClass.NestedClass()<!>

//FILE: b.kt
package b

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

<!EXPOSED_PROPERTY_TYPE!>val mc1 = <!INVISIBLE_MEMBER!>MyJavaClass<!>()<!>

val x = MyJavaClass.<!INVISIBLE_MEMBER!>staticMethod<!>()
val y = MyJavaClass.NestedClass.<!INVISIBLE_MEMBER!>staticMethodOfNested<!>()
<!EXPOSED_PROPERTY_TYPE!>val z = MyJavaClass.<!INVISIBLE_MEMBER!>NestedClass<!>()<!>

//FILE: c.kt
package a.c

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

<!EXPOSED_PROPERTY_TYPE!>val mc1 = <!INVISIBLE_MEMBER!>MyJavaClass<!>()<!>

val x = MyJavaClass.<!INVISIBLE_MEMBER!>staticMethod<!>()
val y = MyJavaClass.NestedClass.<!INVISIBLE_MEMBER!>staticMethodOfNested<!>()
<!EXPOSED_PROPERTY_TYPE!>val z = MyJavaClass.<!INVISIBLE_MEMBER!>NestedClass<!>()<!>