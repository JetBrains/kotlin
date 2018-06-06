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

// 2 INVOKESTATIC Foo\$Erased.empty \(I\)V
// 2 INVOKESTATIC Foo\$Erased.withParam \(ILjava/lang/String;\)V
// 2 INVOKESTATIC Foo\$Erased.withInlineClassParam \(II\)V
// 0 INVOKEVIRTUAL