// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES

// FILE: JavaAbstractVal.java
public abstract value class JavaAbstractVal {
    public abstract int get();
}

// FILE: JavaAbstractIdentity.java
public abstract class JavaAbstractIdentity {}

// FILE: test.kt
value class ValueChild(val x: Int) : JavaAbstractVal() {
    override fun get(): Int = x
}

abstract value class AbstractValueChild : JavaAbstractVal()

class IdentityChild : JavaAbstractVal() {
    override fun get(): Int = 0
}

value class ValueChildOfIdentity(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>JavaAbstractIdentity<!>()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, javaType, override, primaryConstructor,
propertyDeclaration, value */
