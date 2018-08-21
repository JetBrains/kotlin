// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int)

object Test {
    fun asParam(a: Foo) {}
    fun asReturn(): Foo = TODO()
    fun Foo.asExtension() {}
    fun Foo.asAll(x: Any?, a: Foo, b: Int): Foo = TODO()
}

// method: Test::asParam-1e4ch6lh
// jvm signature: (I)V
// generic signature: null

// method: Test::asReturn
// jvm signature: ()I
// generic signature: null

// method: Test::asExtension-1e4ch6lh
// jvm signature: (I)V
// generic signature: null

// method: Test::asAll-dmjdpekg
// jvm signature: (ILjava/lang/Object;II)I
// generic signature: null
