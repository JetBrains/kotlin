// FILE: j/JavaPublic.java
package j;

public class JavaPublic {
    public static void javaM() {}
    public static int javaP = 4;
    static int javaPackageLocal = 5;
}

// FILE: j/JavaPackageLocal.java
package j;

class JavaPackageLocal {
    static void javaMPackage() {}
    static int javaPPackage = 4;
}

// FILE: j/JavaProtected.java
package j;

public class JavaProtected {
    protected static void javaMProtected() {}
    protected static int javaPProtected = 4;
}

// FILE: j/JavaPrivate.java
package j;

public class JavaPrivate {
    private static void javaMPrivate() {}
    private static int javaPPrivate = 4;
}


// FILE: k1.kt
package k

import j.JavaPublic
import j.JavaPublic.javaM
import j.JavaPublic.javaP
import j.JavaPublic.javaPackageLocal

import j.JavaPackageLocal
import j.JavaPackageLocal.javaMPackage
import j.JavaPackageLocal.javaPPackage

import j.JavaProtected
import j.JavaProtected.javaMProtected
import j.JavaProtected.javaPProtected

import j.JavaPrivate
import j.JavaPrivate.javaMPrivate
import j.JavaPrivate.javaPPrivate

// FILE: k2.kt
package j

import j.JavaPublic
import j.JavaPublic.javaM
import j.JavaPublic.javaP
import j.JavaPublic.javaPackageLocal

import j.JavaPackageLocal
import j.JavaPackageLocal.javaMPackage
import j.JavaPackageLocal.javaPPackage

import j.JavaProtected
import j.JavaProtected.javaMProtected
import j.JavaProtected.javaPProtected

import j.JavaPrivate
import j.JavaPrivate.javaMPrivate
import j.JavaPrivate.javaPPrivate