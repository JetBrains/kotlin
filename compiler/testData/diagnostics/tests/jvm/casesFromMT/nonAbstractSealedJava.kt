// RUN_PIPELINE_TILL: BACKEND
// JDK_KIND: FULL_JDK_17
// JVM_TARGET: 17
// FILE: NonAbstractSealed.java
// Java permits a non-abstract sealed class. Kotlin needs the `isJavaNonAbstractSealed`
// flag (set by FirJavaFacade) to relax its "sealed implies abstract" expectation.
public sealed class NonAbstractSealed permits Sub {
    public String name() { return "base"; }
}

// FILE: Sub.java
public final class Sub extends NonAbstractSealed {
    @Override public String name() { return "sub"; }
}

// FILE: useSite.kt
// Instantiating the non-abstract sealed parent directly should be allowed; FIR rejects this
// when the JavaNonAbstractSealed flag isn't propagated, because it then assumes the class is abstract.
fun makeBase(): NonAbstractSealed = NonAbstractSealed()

fun pickName(b: NonAbstractSealed): String = when (b) {
    is Sub -> b.name()
    else -> b.name()
}

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, javaFunction, javaType, smartcast, whenExpression */
