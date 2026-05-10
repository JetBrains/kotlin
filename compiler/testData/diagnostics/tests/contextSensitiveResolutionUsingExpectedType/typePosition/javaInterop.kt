// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: JClass.java
public class JClass {
    public static class JCOption1 extends JClass {
        public int prop1 = 1;
    }
    public static class JCOption2 extends JClass {
        public int prop2 = 2;
    }
    public static SealedJClass staticProp = null;
    public class Nested extends JClass {
        public int prop = 1;
    }
}

// FILE: SealedJClass.java
public sealed abstract class SealedJClass permits SealedJClass.SCOption1, SealedJClass.SCOption2 {
    public static final class SCOption1 extends SealedJClass {
        public int prop1 = 1;
    }
    public static final class SCOption2 extends SealedJClass {
        public int prop2 = 2;
    }
}

// FILE: test.kt
fun testNonsealedJClass(instance: JClass): Int = <!NO_ELSE_IN_WHEN!>when<!> (instance) {
    is <!UNRESOLVED_REFERENCE!>JCOption1<!> -> instance.<!UNRESOLVED_REFERENCE!>prop1<!>;
    is <!UNRESOLVED_REFERENCE!>JCOption2<!> -> instance.<!UNRESOLVED_REFERENCE!>prop2<!>;
    is <!UNRESOLVED_REFERENCE!>Nested<!> -> instance.<!UNRESOLVED_REFERENCE!>prop<!>;
}

fun testSealedJClassInIf(instance: SealedJClass): Int {
    if (instance is SCOption1) {
        return instance.prop1
    }
    if (instance is SCOption2) {
        return instance.prop2
    }
    return 0
}

fun testSealedJClassInWhenWithSubject(instance: SealedJClass): Int = when (instance) {
    is SCOption1 -> instance.prop1
    is SCOption2 -> instance.prop2
}

fun testSealedJClassInWhen(instance: SealedJClass): Int = when {
    instance is SCOption1 -> instance.prop1
    instance is SCOption2 -> instance.prop2
    else -> 0
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, isExpression, javaProperty, javaType,
smartcast, whenExpression, whenWithSubject */
