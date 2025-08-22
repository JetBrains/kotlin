// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// LANGUAGE: -ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType

// FILE: a/A.java
package a;
public interface A {
    B b();
}

// FILE: a/B.java
package a;
public interface B {
    void bar();
}

// FILE: a/AImpl.java
package a;

public class AImpl implements A {
    @Override
    public BImpl b() {
        return new BImpl();
    }
}

// FILE: a/BImpl.java
package a;

class BImpl implements B {
    @Override
    public void bar() {}
}

// FILE: main.kt
import a.A
import a.AImpl

fun test1(a: A) {
    if (a is AImpl) {
        (a <!USELESS_CAST!>as A<!>).b().bar() // OK
        <!INFERRED_INVISIBLE_RETURN_TYPE_WARNING!>a.b()<!>.<!INVISIBLE_REFERENCE!>bar<!>()
    }
}

fun test2(aImpl: AImpl) {
    val a: A = aImpl
    (a <!USELESS_CAST!>as A<!>).b().bar() // OK
    a.b().bar() // Works at FE1.0, fails at FIR
}

/* GENERATED_FIR_TAGS: asExpression, flexibleType, functionDeclaration, ifExpression, isExpression, javaFunction,
javaType, localProperty, propertyDeclaration, smartcast */
