// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
expect class LibClass1 { fun foo(): String }
expect class LibClass2 { fun foo(): String }

// MODULE: lib-platform()()(lib-common)
// FILE: LibJava.java
public class LibJava {
    public String foo() {
        return "OK";
    }
}

// FILE: libPlatform.kt
actual typealias LibClass1 = LibJava
actual typealias LibClass2 = LibJava


// MODULE: app-common(lib-common)
fun test_common(lc1: LibClass1, lc2: LibClass2) {
    lc1.foo()
    lc2.foo()
}


// MODULE: app-inter(lib-common)()(app-common)
fun test_inter(lc1: LibClass1, lc2: LibClass2) {
    lc1.foo()
    lc2.foo()
}


// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(lc1: LibClass1, lc2: LibClass2, j: LibJava) {
    lc1.foo()
    lc2.foo()
    j.foo()
}

fun box(): String {
    return "OK"
}
