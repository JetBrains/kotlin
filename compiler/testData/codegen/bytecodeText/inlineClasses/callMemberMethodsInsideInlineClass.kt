// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    fun empty() {}
    fun withParam(a: String) {}
    fun withInlineClassParam(f: Foo) {}

    fun test() {
        empty()
        withParam("hello")
        withInlineClassParam(this)
    }
}

// 1 INVOKESTATIC Foo\.empty \(I\)V
// 1 INVOKESTATIC Foo\.withParam \(ILjava/lang/String;\)V
// 1 INVOKESTATIC Foo\.withInlineClassParam-1e4ch6lh \(II\)V
// 5 INVOKEVIRTUAL