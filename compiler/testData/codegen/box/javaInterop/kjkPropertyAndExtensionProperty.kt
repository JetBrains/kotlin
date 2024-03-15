// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM, JVM_IR

// FILE: J.java
public class J extends D {}

// FILE: JOverridesRegular.java
public class JOverridesRegular extends D {
    @Override
    public int getA() {
        return 1;
    }
}

// FILE: JOverridesExtension.java
public class JOverridesExtension extends D {
    @Override
    public int getA(String $this) {
        return 1;
    }
}

// FILE: JOVerridesBoth.java
public class JOVerridesBoth extends D {
    @Override
    public int getA() {
        return 1;
    }

    @Override
    public int getA(String $this) {
        return 1;
    }
}

// FILE: 1.kt
open class D {
    open val a: Int
        get() = 2

    open val String.a: Int
        get() = 1
}

class F : J() {
    fun test() {
        a
        "".a
    }
}

class F2 : JOverridesRegular() {
    fun test() {
        a
        "".a
    }
}

class F3 : JOverridesExtension() {
    fun test() {
        a
        "".a
    }
}

class F4 : JOVerridesBoth() {
    fun test() {
        a
        "".a
    }
}

fun box(): String {
    F().test()
    F2().test()
    F3().test()
    F4().test()
    return "OK"
}
