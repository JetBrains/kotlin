// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// WITH_STDLIB
// ENABLE_JVM_PREVIEW

// FILE: JavaVal.java
public value class JavaVal {
    public final int x;

    public JavaVal(int x) {
        this.x = x;
    }
}

// FILE: JavaValRecord.java
public value record JavaValRecord(int x) {}

// FILE: JavaAbstractVal.java
public abstract value class JavaAbstractVal {}

// FILE: JavaSealedVal.java
public sealed abstract value class JavaSealedVal permits JavaSealedValImpl {}

// FILE: JavaSealedValImpl.java
public final value class JavaSealedValImpl extends JavaSealedVal {}

// FILE: test.kt
fun <T : <!FINAL_UPPER_BOUND!>JavaVal<!>> bounded(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
}

fun <T : JavaAbstractVal> boundedByAbstract(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
}

fun test(v: JavaVal, r: JavaValRecord, a: JavaAbstractVal, s: JavaSealedVal) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>v<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>JavaVal(1)<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>r<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>a<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>s<!>) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, javaType, lambdaLiteral, typeConstraint,
typeParameter */
