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

// 1 INVOKESTATIC Foo\.empty-impl \(I\)V
// 1 INVOKESTATIC Foo\.withParam-impl \(ILjava/lang/String;\)V
// 1 INVOKESTATIC Foo\.withInlineClassParam-GWb7d6U \(II\)V
// 5 INVOKEVIRTUAL