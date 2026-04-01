// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/A.java
package test;

public class A {
    public static String p = "p";
    public String q = "q";
}

// FILE: box.kt
import kotlin.reflect.KParameter
import kotlin.test.assertEquals
import test.A

fun box(): String {
    assertEquals("<get-p>", A::p.getter.name)
    assertEquals("<set-p>", A::p.setter.name)
    assertEquals("[]", A::p.getter.parameters.toString())
    assertEquals("[parameter #0 null of fun `<set-p>`(kotlin.String!): kotlin.Unit]", A::p.setter.parameters.toString())
    assertEquals("kotlin.String!", A::p.getter.returnType.toString())
    assertEquals("kotlin.Unit", A::p.setter.returnType.toString())

    assertEquals("<get-q>", A::q.getter.name)
    assertEquals("<set-q>", A::q.setter.name)
    assertEquals("[instance parameter of fun test.A.`<get-q>`(): kotlin.String!]", A::q.getter.parameters.toString())
    assertEquals("[instance parameter of fun test.A.`<set-q>`(kotlin.String!): kotlin.Unit, parameter #1 null of fun test.A.`<set-q>`(kotlin.String!): kotlin.Unit]", A::q.setter.parameters.toString())
    assertEquals("kotlin.String!", A::q.getter.returnType.toString())
    assertEquals("kotlin.Unit", A::q.setter.returnType.toString())

    return "OK"
}
