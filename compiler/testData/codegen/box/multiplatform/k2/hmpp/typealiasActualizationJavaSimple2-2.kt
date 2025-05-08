// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_HMPP: JS_IR

// MODULE: lib-common
// FILE: libCommon.kt
expect class BaseCLib {
    fun foo()
}

// MODULE: lib-platform()()(lib-common)
// FILE: BaseJava.java
public class BaseJava {
    public void foo() {}
}

// FILE: libPlatform.kt
actual typealias BaseCLib = BaseJava

// MODULE: app-common(lib-common)
// FILE: appCommon.kt
fun test_common(a: BaseCLib) {
    a.foo()
}

// MODULE: app-platform(lib-platform)()(app-common)
// FILE: appPlatform.kt
fun test_platform(a: BaseCLib, b: BaseJava) {
    a.foo()
    b.foo()
}

fun box(): String {
    test_common(BaseCLib())
    test_platform(BaseCLib(), BaseJava())
    return "OK"
}