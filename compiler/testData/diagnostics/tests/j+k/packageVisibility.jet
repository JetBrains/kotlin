//FILE: a/MyJavaClass.java
package a;

class MyJavaClass {
}

//FILE:a.kt
package a

val mc = MyJavaClass()

//FILE: b.kt
package b

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

val mc1 = <!INVISIBLE_MEMBER!>MyJavaClass<!>()

//FILE: c.kt
package a.c

import a.<!INVISIBLE_REFERENCE!>MyJavaClass<!>

val mc1 = <!INVISIBLE_MEMBER!>MyJavaClass<!>()