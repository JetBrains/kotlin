// !LANGUAGE: +InlineClasses

inline class Foo<T>(val x: List<T>)

object Test {
    fun nonNullTypeArgument(f: Foo<Int>) {}
    fun nullableTypeArgument(f: Foo<String?>) {}

    fun nullableValue(f: Foo<Long>?) {}
}

// method: Test::nonNullTypeArgument-GWb7d6U
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<Ljava/lang/Integer;>;)V

// method: Test::nullableTypeArgument-GWb7d6U
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<Ljava/lang/String;>;)V

// method: Test::nullableValue-N3I3QIo
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<Ljava/lang/Long;>;)V

// method: Foo::box-impl
// jvm signature: (Ljava/util/List;)LFoo;
// generic signature: null

// method: Foo::unbox-impl
// jvm signature: ()Ljava/util/List;
// generic signature: ()Ljava/util/List<TT;>;
