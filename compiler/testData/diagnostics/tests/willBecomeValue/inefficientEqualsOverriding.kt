// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CustomEqualsInValueClasses +FullValueClasses
// WITH_STDLIB
// DIAGNOSTICS: -INVISIBLE_REFERENCE

@JvmInline
value class Inline(val x: Int) {
    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?): Boolean = other is Inline && other.x == x
    override fun hashCode(): Int = x
}

@WillBecomeValue
class Migrating(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Migrating && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Migrating($x)"
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, isExpression,
nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, value */
