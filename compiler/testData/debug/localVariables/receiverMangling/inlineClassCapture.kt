// FILE: test.kt
class Foo {
    fun foo() {
        block {
            this@Foo
        }
    }

    inner class Bar {
        fun bar() {
            block {
                this@Foo
                this@Bar

                block {
                    this@Foo
                    this@Bar
                }
            }
        }
    }
}

inline fun block(block: () -> Unit) = block()

fun box() {
    val x = Foo()
    x.foo()
    val y = x.Bar()
    y.bar()
}

// LOCAL VARIABLES
// TestKt:27:
// Foo:2:
// TestKt:27:
// TestKt:28: x:Foo
// Foo:4:
// Foo:24: $i$f$block:int
// Foo:5: $i$f$block:int, $i$a$-block-Foo$foo$1:int
// Foo:6: $i$f$block:int, $i$a$-block-Foo$foo$1:int
// Foo:7:
// TestKt:29: x:Foo
// Foo$Bar:9:
// TestKt:29: x:Foo
// TestKt:30: x:Foo, y:Foo$Bar
// Foo$Bar:11:
// Foo$Bar:24: $i$f$block:int
// Foo$Bar:12: $i$f$block:int, $i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:13: $i$f$block:int, $i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:15: $i$f$block:int, $i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:24: $i$a$-block-Foo$Bar$bar$1:int, $i$f$block:int
// Foo$Bar:16: $i$a$-block-Foo$Bar$bar$1:int, $i$f$block:int, $i$a$-block-Foo$Bar$bar$1$1:int
// Foo$Bar:17: $i$a$-block-Foo$Bar$bar$1:int, $i$f$block:int, $i$a$-block-Foo$Bar$bar$1$1:int
// Foo$Bar:18: $i$a$-block-Foo$Bar$bar$1:int, $i$f$block:int, $i$a$-block-Foo$Bar$bar$1$1:int
// Foo$Bar:19: $i$f$block:int, $i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:20:
// TestKt:31: x:Foo, y:Foo$Bar