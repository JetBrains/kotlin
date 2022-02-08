// FILE: j/JavaPackageLocal.java
package j;

public class JavaPackageLocal {
    static void javaMPackage() {}
    static int javaPPackage = 4;
}

// FILE: j/JavaProtected.java
package j;

public class JavaProtected {
    protected static void javaMProtectedStatic() {}
    protected static int javaPProtectedStatic = 4;
    protected final int javaPProtectedPackage = 4;
}

// FILE: k.kt
package k

import j.JavaProtected
import j.JavaPackageLocal

class A {
    val p1 = JavaPackageLocal.<!INVISIBLE_MEMBER("javaPPackage; package-private; 'JavaPackageLocal'")!>javaPPackage<!>
    val p2 = JavaProtected.<!INVISIBLE_MEMBER("javaPProtectedStatic; protected; 'JavaProtected'")!>javaPProtectedStatic<!>
    val p3 = JavaProtected().<!INVISIBLE_MEMBER("javaPProtectedPackage; protected; 'JavaProtected'")!>javaPProtectedPackage<!>

    fun test() {
        JavaProtected.<!INVISIBLE_MEMBER("javaMProtectedStatic; protected; 'JavaProtected'")!>javaMProtectedStatic<!>()
        JavaPackageLocal.<!INVISIBLE_MEMBER("javaMPackage; package-private; 'JavaPackageLocal'")!>javaMPackage<!>()
    }
}

class B : JavaProtected() {
    val p1 = JavaPackageLocal.<!INVISIBLE_MEMBER("javaPPackage; package-private; 'JavaPackageLocal'")!>javaPPackage<!>
    val p2 = JavaProtected.javaPProtectedStatic
    val p3 = javaPProtectedPackage

    fun test() {
        JavaProtected.javaMProtectedStatic()
        JavaPackageLocal.<!INVISIBLE_MEMBER("javaMPackage; package-private; 'JavaPackageLocal'")!>javaMPackage<!>()
    }
}

// FILE: j.kt
package j

import j.JavaProtected
import j.JavaPackageLocal

class C {
    val p1 = JavaPackageLocal.javaPPackage
    val p2 = JavaProtected.javaPProtectedStatic
    val p3 = JavaProtected().javaPProtectedPackage

    fun test() {
        JavaProtected.javaMProtectedStatic()
        JavaProtected.javaMProtectedStatic()
        JavaPackageLocal.javaMPackage()
    }
}
