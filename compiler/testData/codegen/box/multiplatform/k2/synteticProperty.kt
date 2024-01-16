// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: Derived.java

public class Derived extends Base {
    public String getFoo() { return "OK"; }
}

// FILE: Base.kt
open class Base {
    open val foo: String
        get() = "FAIL"
}


// MODULE: common(lib)
// FILE: common.kt

val p = Derived().foo

// MODULE: platform(lib)()(common)
// FILE: platform.kt

val q = object : Derived() {
    val s = foo
}

fun box() : String {
    return "OK"
}