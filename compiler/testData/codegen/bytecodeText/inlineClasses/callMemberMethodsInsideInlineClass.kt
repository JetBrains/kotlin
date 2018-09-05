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

// 1 INVOKESTATIC Foo\$Erased.empty \(I\)V
// 1 INVOKESTATIC Foo\$Erased.withParam \(ILjava/lang/String;\)V
// 1 INVOKESTATIC Foo\$Erased.withInlineClassParam-1e4ch6lh \(II\)V
// 5 INVOKEVIRTUAL