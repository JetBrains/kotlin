// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_11
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;

    requires kotlin.stdlib;
}

// FILE: a/A.java
package a;

import a.impl.AImpl;

public class A {
    public static AImpl getInstance() {
        return new AImpl();
    }
}

// FILE: a/K.kt
package a

import a.impl.KImpl

open class K {
    companion object {
        fun getInstance(): KImpl = KImpl()
    }
}

// FILE: a/impl/AImpl.java
package a.impl;

import a.A;

public class AImpl extends A {
    public static String field = "";
    public static String method() { return ""; }
}

// FILE: a/impl/KImpl.kt
package a.impl

import a.K

class KImpl : K() {
    companion object {
        val field: String = ""
        fun method(): String = ""
    }
}

// FILE: a/impl/KotlinFile.kt
package a.impl

val fileField: String = ""
fun fileMethod(): String = ""

// MODULE: moduleB(moduleA)
// FILE: module-info.java
module moduleB {
    requires moduleA;

    requires kotlin.stdlib;
}

// FILE: usage.kt
package test

import a.*
import a.impl.*

val a1: A = A()
val a2: A = A.getInstance()
val a3: <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>AImpl<!> = A.getInstance()
val a4: String = <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>AImpl<!>.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>method<!>()
val a5: String = <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>AImpl<!>.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>field<!>

val k1: K = K()
val k2: K = K.getInstance()
val k3: <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>KImpl<!> = K.getInstance()
val k4: String = <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>KImpl<!>.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>method<!>()
val k5: String = <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>KImpl<!>.<!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>field<!>

val kf1: String = <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>fileField<!>
val kf2: String = <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>fileMethod<!>()
