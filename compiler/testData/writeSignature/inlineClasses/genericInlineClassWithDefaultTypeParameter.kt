// !LANGUAGE: +InlineClasses

@Suppress("INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE")
inline class Default<T>(val x: T)

class Inv<T>

object Test {
    fun withNotNullPrimitive(a: Default<Int>) {}
    fun withAdditionalGenericParameter(x: Inv<String>, y: Default<String>) {}

    fun asNullable(a: Default<Int>?) {}

    fun asNullableTypeArgument(a: Default<Int?>) {}
    fun asNullableAndNullableTypeArgument(a: Default<Int?>?) {}
}

// method: Test::withNotNullPrimitive-jBNXRxo
// jvm signature: (I)V
// generic signature: null

// method: Test::withAdditionalGenericParameter-HGK7qdE
// jvm signature: (LInv;Ljava/lang/String;)V
// generic signature: (LInv<Ljava/lang/String;>;Ljava/lang/String;)V

// method: Test::asNullable-wrrn6tY
// jvm signature: (LDefault;)V
// generic signature: (LDefault<Ljava/lang/Integer;>;)V

// method: Test::asNullableTypeArgument-jBNXRxo
// jvm signature: (Ljava/lang/Integer;)V
// generic signature: null

// method: Test::asNullableAndNullableTypeArgument-wrrn6tY
// jvm signature: (LDefault;)V
// generic signature: (LDefault<Ljava/lang/Integer;>;)V

// method: Default:box
// jvm signature: (Ljava/lang/Object;)LDefault;
// generic signature: null

// method: Default:unbox
// jvm signature: ()Ljava/lang/Object
// generic signature: null
