// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

// FILE: p/J.java

package p;

public class J {
    public J j() {return null;}

    public <T> T foo() {return null;}
    public <T extends J> T foo1() {return null;}
}

// FILE: k.kt

import p.*

fun test(j: J) {
    checkSubtype<J>(j.j())
    j.j().j()
    j.j()!!.j()

    val ann = j.foo<String>()
    ann!!.length
    ann.length

    val a = j.foo<J>()
    a!!.j()
    a.j()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, javaFunction, javaType, localProperty, nullableType, propertyDeclaration, smartcast,
typeParameter, typeWithExtension */
