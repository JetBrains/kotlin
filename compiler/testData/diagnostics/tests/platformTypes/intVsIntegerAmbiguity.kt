// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: p/J.java

package p;

public class J {
    public interface A {}
    public static A foo(int s);

    public interface B {}
    public static B foo(Integer s);

    public static Integer getInteger();
}

// FILE: k.kt

import p.*
import p.J.*

class C

fun foo(i: Int?) : C = null!!

fun test(i: Int, ni: Int?) {
    checkSubtype<C>(foo(2))
    checkSubtype<J.A>(J.foo(2))
    checkSubtype<J.A>(J.foo(i))
    checkSubtype<J.B>(J.foo(ni))
    checkSubtype<C>(foo(ni))
    checkSubtype<J.B>(J.foo(ni))

    foo(J.getInteger())
    J.foo(J.getInteger())
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, javaFunction, javaType, nullableType, typeParameter, typeWithExtension */
