// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    inline fun inlineInc(): Foo = Foo(x + 1) // one actual call inside wrapper class Foo
    fun notInlineInc(): Foo = Foo(x + 1)

    fun foo() {
        inlineInc()
    }
}

fun test(f: Foo) {
    f.inlineInc().inlineInc().inlineInc()
    f.notInlineInc() // one here, one inside wrapper class Foo
}

// 1 INVOKESTATIC Foo\$Erased.inlineInc
// 2 INVOKESTATIC Foo\$Erased.notInlineInc
