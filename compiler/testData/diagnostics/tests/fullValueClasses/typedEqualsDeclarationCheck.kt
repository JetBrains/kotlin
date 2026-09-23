// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +FullValueClasses +CustomEqualsInValueClasses

value class OverridesEquals(val x: Int) {
    override fun equals(other: Any?): Boolean = other is OverridesEquals && x == other.x
    override fun hashCode(): Int = x
}

value class OverridesEqualsWithTypedEquals(val x: Int) {
    override fun equals(other: Any?): Boolean = other is OverridesEqualsWithTypedEquals && equals(other)
    fun equals(other: OverridesEqualsWithTypedEquals): Boolean = x == other.x
    override fun hashCode(): Int = x
}

abstract value class AbstractOverridesEquals {
    override fun equals(other: Any?): Boolean = other is AbstractOverridesEquals
    override fun hashCode(): Int = 0
}

value object ObjectOverridesEquals {
    override fun equals(other: Any?): Boolean = other is ObjectOverridesEquals
    override fun hashCode(): Int = 0
}

value class TypedEqualsWithTypeArgument<T>(val x: T) {
    fun equals(other: TypedEqualsWithTypeArgument<T>): Boolean = x == other.x
}

value class TypedEqualsWithTypeParameter(val x: Int) {
    fun <S> equals(other: TypedEqualsWithTypeParameter): Boolean = x == other.x
}

@JvmInline
value class InlineOverridesEquals(val x: Int) {
    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS("InlineOverridesEquals")!>equals<!>(other: Any?): Boolean = other is InlineOverridesEquals && x == other.x
    override fun hashCode(): Int = x
}

@JvmInline
value class InlineOverridesEqualsWithTypedEquals(val x: Int) {
    override fun equals(other: Any?): Boolean = other is InlineOverridesEqualsWithTypedEquals && equals(other)
    fun equals(other: InlineOverridesEqualsWithTypedEquals): Boolean = x == other.x
    override fun hashCode(): Int = x
}

@JvmInline
value class InlineTypedEqualsWithTypeArgument<T>(val x: T) {
    fun equals(other: <!TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS!>InlineTypedEqualsWithTypeArgument<T><!>): Boolean = x == other.x
}

@JvmInline
value class InlineTypedEqualsWithTypeParameter(val x: Int) {
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><S><!> equals(other: InlineTypedEqualsWithTypeParameter): Boolean = x == other.x
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, smartcast,
typeParameter, value */
