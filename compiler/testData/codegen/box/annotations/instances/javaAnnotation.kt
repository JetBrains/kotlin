// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// FILE: A.java

public @interface A {}

// FILE: B.java

public @interface B {
    String value();
}

// FILE: C.java

public @interface C {
    int[] v1();
    String v2();
}

// FILE: D.java

public @interface D {
    String value() default "hello";
}

// FILE: b.kt

fun box(): String {
    val a = A()
    val b = B("OK")
    assert(b.value == "OK")
    val c = C(v2 = "v2", v1 = intArrayOf(1))
    assert(c.v2 == "v2")
    // TODO(KT-47702): Looks like we have to force users either to pass default java parameters explicitly
    // or hack LazyJavaClassDescriptor/JavaPropertyDescriptor to load annotation param default value,
    // because it is not stored currently anywhere.
    // val d = D()
    val d = D("OK").value
    return d
}
