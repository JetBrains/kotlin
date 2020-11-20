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

// JVM_TEMPLATES
// 1 final synthetic LMultipleExtensionReceiversKt\$test\$1; this\$0
// 1 final synthetic LBar; \$this_test

// JVM_IR_TEMPLATES
// 1 final synthetic LFoo; \$this_test
// 1 final synthetic LBar; \$this_test\$1