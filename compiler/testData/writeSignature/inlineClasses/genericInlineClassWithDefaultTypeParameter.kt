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

// method: Test::withNotNullPrimitive-7odoyk9m
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::withAdditionalGenericParameter-1k09dck1
// jvm signature: (LInv;Ljava/lang/Object;)V
// generic signature: (LInv<Ljava/lang/String;>;Ljava/lang/Object;)V

// method: Test::asNullable-ao7usvyu
// jvm signature: (LDefault;)V
// generic signature: (LDefault<Ljava/lang/Integer;>;)V

// method: Test::asNullableTypeArgument-7odoyk9m
// jvm signature: (Ljava/lang/Object;)V
// generic signature: null

// method: Test::asNullableAndNullableTypeArgument-ao7usvyu
// jvm signature: (LDefault;)V
// generic signature: (LDefault<Ljava/lang/Integer;>;)V

// method: Default$Erased:box
// jvm signature: (Ljava/lang/Object;)LDefault;
// generic signature: null

// method: Default:unbox
// jvm signature: ()Ljava/lang/Object
// generic signature: null
