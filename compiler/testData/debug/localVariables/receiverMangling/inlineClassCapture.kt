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
// TestKt:28: LV:x:Foo
// Foo:4:
// Foo:24: LV:$i$f$block:int
// Foo:5: LV:$i$f$block:int, LV:$i$a$-block-Foo$foo$1:int
// Foo:6: LV:$i$f$block:int, LV:$i$a$-block-Foo$foo$1:int
// Foo:7:
// TestKt:29: LV:x:Foo
// Foo$Bar:9: F:this$0:null
// TestKt:29: LV:x:Foo
// TestKt:30: LV:x:Foo, LV:y:Foo$Bar
// Foo$Bar:11: F:this$0:Foo
// Foo$Bar:24: F:this$0:Foo, LV:$i$f$block:int
// Foo$Bar:12: F:this$0:Foo, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:13: F:this$0:Foo, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:15: F:this$0:Foo, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:24: F:this$0:Foo, LV:$i$a$-block-Foo$Bar$bar$1:int, LV:$i$f$block:int
// Foo$Bar:16: F:this$0:Foo, LV:$i$a$-block-Foo$Bar$bar$1:int, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1$1:int
// Foo$Bar:17: F:this$0:Foo, LV:$i$a$-block-Foo$Bar$bar$1:int, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1$1:int
// Foo$Bar:18: F:this$0:Foo, LV:$i$a$-block-Foo$Bar$bar$1:int, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1$1:int
// Foo$Bar:19: F:this$0:Foo, LV:$i$f$block:int, LV:$i$a$-block-Foo$Bar$bar$1:int
// Foo$Bar:20: F:this$0:Foo
// TestKt:31: LV:x:Foo, LV:y:Foo$Bar