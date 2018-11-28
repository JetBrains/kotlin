// !LANGUAGE: +InlineClasses

@Suppress("INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE")
inline class NonNull<T : Any>(val x: T)

@Suppress("INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE")
inline class NullableValue<T : Any>(val x: T?)

object Test {
    fun withNotNullPrimitive(a: NonNull<Int>) {}
    fun asNullable(a: NonNull<Int>?) {}

    fun withNotNullForNullableValue(a: NullableValue<Int>) {}
    fun asNullableForNullableValue(a: NullableValue<Int>?) {}
}

// method: Test::withNotNullPrimitive-inxE8tU
// jvm signature: (I)V
// generic signature: null

// method: Test::asNullable-UczwCtY
// jvm signature: (LNonNull;)V
// generic signature: (LNonNull<Ljava/lang/Integer;>;)V

// method: Test::withNotNullForNullableValue-3nNnbBk
// jvm signature: (Ljava/lang/Integer;)V
// generic signature: null

// method: Test::asNullableForNullableValue-wXDnar0
// jvm signature: (LNullableValue;)V
// generic signature: (LNullableValue<Ljava/lang/Integer;>;)V