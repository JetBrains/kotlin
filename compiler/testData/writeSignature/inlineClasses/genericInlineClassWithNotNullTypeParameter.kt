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

// method: Test::withNotNullPrimitive-7l8qu2mt
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::asNullable-4hed6sie
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::withNotNullForNullableValue-c6wvqrdl
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::asNullableForNullableValue-aloai6d9
// jvm signature: (LNullableValue;)V
// generic signature: (LNullableValue<Ljava/lang/Integer;>;)V