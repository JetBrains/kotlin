// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// SKIP_JAVAC

// FILE: jdk/internal/ValueBased.java
package jdk.internal;

public @interface ValueBased {}

// FILE: JavaValueBasedBase.java
@jdk.internal.ValueBased
public abstract class JavaValueBasedBase {}

// FILE: test.kt
abstract value class ValueBase

@WillBecomeValue
abstract class WillBecomeValueBase

value class FromValueBase(val x: Int) : ValueBase()
value class FromWillBecomeValueBase(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>WillBecomeValueBase<!>()
value class FromJavaValueBasedBase(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>JavaValueBasedBase<!>()

@WillBecomeValue
class WillBecomeValueFromValueBase(val x: Int) : ValueBase() {
    override fun equals(other: Any?): Boolean = other is WillBecomeValueFromValueBase && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "WillBecomeValueFromValueBase($x)"
}

@WillBecomeValue
class WillBecomeValueFromWillBecomeValueBase(val x: Int) : WillBecomeValueBase() {
    override fun equals(other: Any?): Boolean = other is WillBecomeValueFromWillBecomeValueBase && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "WillBecomeValueFromWillBecomeValueBase($x)"
}

@WillBecomeValue
class WillBecomeValueFromJavaValueBasedBase(val x: Int) : JavaValueBasedBase() {
    override fun equals(other: Any?): Boolean = other is WillBecomeValueFromJavaValueBasedBase && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "WillBecomeValueFromJavaValueBasedBase($x)"
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, isExpression, javaType,
nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, value */
