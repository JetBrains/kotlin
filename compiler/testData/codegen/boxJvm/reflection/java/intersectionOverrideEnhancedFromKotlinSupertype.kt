// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.kt
package test

class Box<T>

interface A<T> {
    fun bar(): Box<T>
}

interface B {
    fun foo(w: Box<String>)
    fun bar(): Box<String>
}

// FILE: test/C.java
package test;

public abstract class C<T> implements A<T> {
    public void foo(Box<T> w) {}
    public Box<T> bar() { return null; }
}

// FILE: test/D.java
package test;

public abstract class D extends C<String> implements B {
}

// FILE: box.kt
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

class Z : D()

private fun check(expected: String, klass: KClass<*>, name: String) {
    assertEquals(expected, klass.members.single { it.name == name }.toString())
}

fun box(): String {
    check("fun test.C<T>.foo(test.Box<T!>!): kotlin.Unit", C::class, "foo")
    check("fun test.C<T>.bar(): test.Box<T!>", C::class, "bar")

    check("fun test.D.foo(test.Box<kotlin.String>): kotlin.Unit", D::class, "foo")
    check("fun test.D.bar(): test.Box<kotlin.String>", D::class, "bar")

    check("fun test.Z.foo(test.Box<kotlin.String>): kotlin.Unit", Z::class, "foo")
    check("fun test.Z.bar(): test.Box<kotlin.String>", Z::class, "bar")

    return "OK"
}
