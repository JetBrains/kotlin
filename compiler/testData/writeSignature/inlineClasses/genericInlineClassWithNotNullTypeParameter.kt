// !LANGUAGE: +InlineClasses

inline class NonNull<T : Any>(val x: T)
inline class NullableValue<T : Any>(val x: T?)

object Test {
    fun withNotNullPrimitive(a: NonNull<Int>) {}
    fun asNullable(a: NonNull<Int>?) {}

    fun withNotNullForNullableValue(a: NullableValue<Int>) {}
    fun asNullableForNullableValue(a: NullableValue<Int>?) {}
}

// method: Test::withNotNullPrimitive
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::asNullable
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::withNotNullForNullableValue
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::asNullableForNullableValue
// jvm signature: (LNullableValue;)V
// generic signature: (LNullableValue<Ljava/lang/Integer;>;)V