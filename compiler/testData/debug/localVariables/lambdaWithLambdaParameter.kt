
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

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:21 box:
// test.kt:3 <init>:
// test.kt:4 <init>:
// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:3 <init>:
// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:21 box:
// test.kt:22 box: foo:Foo=Foo
// test.kt:13 start:
// test.kt:7 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int
// test.kt:14 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int, it\2:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1\2\101\0:int=0:int
// test.kt:15 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int, it\2:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1\2\101\0:int=0:int
// test.kt:8 invoke:
// test.kt:4 getBar:
// test.kt:8 invoke:
// test.kt:4 setBar: <set-?>:java.lang.String="OK":java.lang.String
// test.kt:9 invoke:
// test.kt:15 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int, it\2:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1\2\101\0:int=0:int
// test.kt:16 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int, it\2:kotlin.jvm.functions.Function0=Foo$inlineCall$1, $i$a$-inlineCall-Foo$start$1\2\101\0:int=0:int
// test.kt:7 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int
// test.kt:10 start: this_\1:Foo=Foo, $i$f$inlineCall\1\13:int=0:int
// test.kt:17 start:
// test.kt:23 box: foo:Foo=Foo


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

// EXPECTATIONS WASM
// test.kt:21 $box: $foo:(ref null $Foo)=null (14, 14)
// test.kt:4 $Foo.<init>: $<this>:(ref $Foo)=(ref $Foo) (14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14)
// test.kt:18 $Foo.<init>: $<this>:(ref $Foo)=(ref $Foo) (1, 1, 1)
// test.kt:22 $box: $foo:(ref $Foo)=(ref $Foo) (4, 8)
// test.kt:13 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (8, 8, 8, 8, 8)
// test.kt:7 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (15, 15, 15, 15, 8, 8, 8)
// test.kt:14 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (12, 12, 12, 19, 19, 19, 12, 12)
// test.kt:15 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (12, 12, 12, 12, 12, 12, 12, 12, 12, 12)
// test.kt:8 $Foo$start$lambda.invoke: $<this>:(ref $Foo$start$lambda)=(ref $Foo$start$lambda) (12, 12, 12, 12, 12, 12, 12, 19, 19, 19, 12, 12, 22)
// test.kt:9 $Foo$start$lambda.invoke: $<this>:(ref $Foo$start$lambda)=(ref $Foo$start$lambda) (9)
// test.kt:15 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (12, 12)
// test.kt:16 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (9, 9, 9)
// test.kt:10 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (5, 5)
// test.kt:17 $Foo.start: $<this>:(ref $Foo)=(ref $Foo) (5, 5)
// test.kt:23 $box: $foo:(ref $Foo)=(ref $Foo) (1, 1)
