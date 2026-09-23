// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +FullValueClasses +CustomEqualsInValueClasses

value class SingleField(val x: Int) {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must override 'equals()' in Any")!>operator<!> fun equals(other: SingleField): Boolean = x == other.x
}

value class MultiField(val x: Int, val y: Int) {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must override 'equals()' in Any")!>operator<!> fun equals(other: MultiField): Boolean = x == other.x && y == other.y
}

value class Generic<T>(val x: T) {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must override 'equals()' in Any")!>operator<!> fun equals(other: Generic<*>): Boolean = x == other.x
}

abstract value class Abstract {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must override 'equals()' in Any")!>operator<!> fun equals(other: Abstract): Boolean = true
}

value object ValueObject {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must override 'equals()' in Any")!>operator<!> fun equals(other: ValueObject): Boolean = true
}

@JvmInline
value class Inline(val x: Int) {
    operator fun equals(other: Inline): Boolean = x == other.x
}

/* GENERATED_FIR_TAGS: andExpression, capturedType, classDeclaration, equalityExpression, functionDeclaration,
nullableType, objectDeclaration, operator, primaryConstructor, propertyDeclaration, starProjection, typeParameter, value */
