// !LANGUAGE: +InlineClasses

inline class Foo(val b: Bar)
inline class Bar(val i: Int)

object Test {
    fun simple(f: Foo) {}
    fun listOfFoo(f: List<Foo>) {}
}

// method: Test::simple-1e4ch6lh
// jvm signature: (I)V
// generic signature: null

// method: Test::listOfFoo
// jvm signature: (Ljava/util/List;)V
// generic signature: (Ljava/util/List<LFoo;>;)V
