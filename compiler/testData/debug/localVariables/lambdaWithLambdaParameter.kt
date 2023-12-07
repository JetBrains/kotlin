// IGNORE_INLINER: IR
// FILE: test.kt
class Foo {
    var bar = ""

    inline fun inlineCall(action: (complete: () -> Unit) -> Unit) {
        action {
            bar += "K"
        }
    }

    fun start() {
        inlineCall {
            bar += "O"
            it()
        }
    }
}

fun box() {
    val foo = Foo()
    foo.start()
}

// EXPECTATIONS JVM_IR
// test.kt:21 box:
// test.kt:3 <init>:
// test.kt:4 <init>:
// test.kt:3 <init>:
// test.kt:21 box:
// test.kt:22 box: foo:Foo=Foo
// test.kt:13 start:
// test.kt:7 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int
// test.kt:14 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int, it:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1:int=0:int
// test.kt:15 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int, it:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1:int=0:int
// test.kt:8 invoke:
// test.kt:4 getBar:
// test.kt:8 invoke:
// test.kt:4 setBar: <set-?>:java.lang.String="OK":java.lang.String
// test.kt:9 invoke:
// test.kt:15 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int, it:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1:int=0:int
// test.kt:16 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int, it:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1:int=0:int
// test.kt:7 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int
// test.kt:10 start: this_$iv:Foo=Foo, $i$f$inlineCall:int=0:int
// test.kt:17 start:
// test.kt:23 box: foo:Foo=Foo

// EXPECTATIONS JS_IR
// test.kt:21 box:
// test.kt:4 <init>:
// test.kt:3 <init>:
// test.kt:22 box: foo=Foo
// test.kt:14 start:
// test.kt:8 start:
// test.kt:17 start:
// test.kt:23 box: foo=Foo
