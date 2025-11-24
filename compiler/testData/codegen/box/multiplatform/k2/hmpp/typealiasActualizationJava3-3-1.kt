// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
expect class LibClass3 { fun foo(): String }

// MODULE: lib-inter()()(lib-common)
expect class LibInterClass3 { fun foo(): String }
actual typealias LibClass3 = LibInterClass3


// MODULE: lib-platform()()(lib-inter)
// FILE: LibJava3.java
public class LibJava3 {
    public String foo() {
        return "2";
    }
}

// FILE: libPlatform.kt
actual typealias LibInterClass3 = LibJava3

// MODULE: app-common(lib-common)
fun test_common(lc3: LibClass3) {
    lc3.foo()
}

// MODULE: app-inter(lib-inter)()(app-common)
fun test_inter(lc3: LibClass3) {
    lc3.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(lc3: LibClass3, lic3: LibInterClass3) {
    lc3.foo()
    lic3.foo()
}

fun box(): String {
    return "OK"
}
