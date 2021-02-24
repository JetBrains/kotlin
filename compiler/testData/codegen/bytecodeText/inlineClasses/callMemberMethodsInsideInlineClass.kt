// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TODO KT-36783 Consider generating GETFIELD instructions instead of unbox-impl calls in special methods of inline classes in JVM_IR

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
// 2 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(I\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder.toString \(\)Ljava/lang/String;
// 1 INVOKEVIRTUAL Foo.unbox-impl \(\)I