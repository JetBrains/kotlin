// !LANGUAGE: +InlineClasses

inline class InlinePrimitive(val x: Int)
inline class InlineReference(val y: String)
inline class InlineNullablePrimitive(val x: Int?)
inline class InlineNullableReference(val y: String?)

object Test {
    fun withPrimitiveAsNullable(a: InlinePrimitive?) {}
    fun withReferenceAsNullable(a: InlineReference?) {}

    fun withNullablePrimitiveAsNullable(a: InlineNullablePrimitive?) {}
    fun withNullableReferenceAsNullable(a: InlineNullableReference?) {}
}

// method: Test::withPrimitiveAsNullable-arwt9fzf
// jvm signature: (LInlinePrimitive;)V
// generic signature: null

// method: Test::withReferenceAsNullable-8k1ogbuu
// jvm signature: (Ljava/lang/String;)V
// generic signature: null

// method: Test::withNullablePrimitiveAsNullable-aiqm4cvc
// jvm signature: (LInlineNullablePrimitive;)V
// generic signature: null

// method: Test::withNullableReferenceAsNullable-7pmrpo2y
// jvm signature: (LInlineNullableReference;)V
// generic signature: null