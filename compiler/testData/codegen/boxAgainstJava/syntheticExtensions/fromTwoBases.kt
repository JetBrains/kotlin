// FILE: C.java

interface A {
    String getOk();
}

interface B {
    String getOk();
}

interface C extends A, B {
}

// FILE: JavaClass.java

class JavaClass implements C {
    public String getOk() { return "OK"; }
}

// FILE: 1.kt

fun box(): String {
    return f(JavaClass())
}

internal fun f(c: C) = c.ok
