// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// FILE: J.java

@kotlin.WillBecomeValue
public final class J {
    public static J make() { return new J(); }

    @Override public boolean equals(Object other) { return other instanceof J; }
    @Override public int hashCode() { return 0; }
    @Override public String toString() { return "J"; }
}

// FILE: test.kt

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

@WillBecomeValue
abstract class Recursive<T : Recursive<T>>

fun flexible() = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>J.make() === J.make()<!>

fun flexibleLock() = synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>J.make()<!>) {}

fun captured(list: MutableList<out Wrapper>, w: Wrapper) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>list[0] === w<!>

fun <T : Wrapper?> definitelyNotNull(a: T & Any, b: T & Any) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a === b<!>

fun <T : Recursive<T>> recursiveBound(a: T, b: Recursive<T>) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a === b<!>

fun starProjected(list: List<*>, w: Wrapper) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>list[0] === w<!>

fun nothing(w: Wrapper) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>w === TODO()<!>

fun inLambda(a: Wrapper, b: Wrapper) = run { <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a === b<!> }

/* GENERATED_FIR_TAGS: andExpression, capturedType, classDeclaration, dnnType, equalityExpression, flexibleType,
functionDeclaration, integerLiteral, isExpression, javaFunction, lambdaLiteral, nullableType, operator, outProjection,
override, primaryConstructor, propertyDeclaration, smartcast, starProjection, stringLiteral, typeConstraint,
typeParameter */
