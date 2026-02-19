class Foo {
    fun foo() {}
}

class Bar {
    fun bar() {}
}

fun Foo.test(bar: Bar) {
    fun Bar.test() {
        class Local {
            fun run() {
                foo()
                bar()
            }
        }
    }
}

// 1 final synthetic LFoo; \$this_test
// 1 final synthetic LBar; \$this_test\$1
