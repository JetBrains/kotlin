// RUN_PIPELINE_TILL: BACKEND
// JDK_KIND: FULL_JDK_17
// JVM_TARGET: 17
// FILE: Base.java

public sealed class Base permits Sub1, Sub2 {}

// FILE: Sub1.java

public final class Sub1 extends Base {}

// FILE: Sub2.java

public final class Sub2 extends Base {}

// FILE: useSite.kt
fun bar(b: Base): String = when (b) {
    is Sub1 -> "1"
    is Sub2 -> "2"
}

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, javaType, smartcast, whenExpression, whenWithSubject */
