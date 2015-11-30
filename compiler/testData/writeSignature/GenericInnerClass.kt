class Outer<E, F : Number> {
    inner class Inner<G, H>
}

object Foo {
    fun <Y> foo(x: Outer<String, Int>.Inner<Y, CharSequence>) = 1
}

// class: Outer
// jvm signature:     Outer
// generic signature: <E:Ljava/lang/Object;F:Ljava/lang/Number;>Ljava/lang/Object;

// class: Outer$Inner
// jvm signature:     Outer$Inner
// generic signature: <G:Ljava/lang/Object;H:Ljava/lang/Object;>Ljava/lang/Object;

// method: Foo::foo
// jvm signature: (LOuter$Inner;)I
// generic signature: <Y:Ljava/lang/Object;>(LOuter<Ljava/lang/String;Ljava/lang/Integer;>.Inner<TY;Ljava/lang/CharSequence;>;)I
