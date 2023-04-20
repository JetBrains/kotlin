
// FILE: test.kt

data class A(val x: Double = 1.0, val y: String = "", val z: Char = '0')

fun foo(a: A, block: (A, String, Int) -> String): String = block(a, "", 1)

val arrayOfA: Array<A> = Array(1) { A() }

fun box() {

    foo(A()) {
        (x, _, y), _, w ->

        val (a, _, c) = A()
        val (_, `_`, d) = A()

        for ((_, q) in arrayOfA) {

        }

        ""
    }
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:12 box:
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:12 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// test.kt:13 invoke: w:int=1:int
// test.kt:15 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:15 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:16 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:16 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:18 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:8 getArrayOfA:
// test.kt:18 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:22 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// test.kt:12 box:
// test.kt:24 box:

// EXPECTATIONS FIR JVM_IR
// test.kt:12 box:
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:12 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$<lambda>
// test.kt:13 box$lambda$0: w:int=1:int
// test.kt:15 box$lambda$0: w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:15 box$lambda$0: w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:16 box$lambda$0: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:16 box$lambda$0: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:18 box$lambda$0: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:22 box$lambda$0: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$<lambda>
// test.kt:12 box:
// test.kt:24 box:

// EXPECTATIONS JS_IR
// test.kt:8 <init properties test.kt>:
// test.kt:8 <init properties test.kt>:
// test.kt:8 <init properties test.kt>:
// test.kt:4 <init>: x=1:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:8 <init properties test.kt>:
// test.kt:8 <init properties test.kt>:
// test.kt:8 <init properties test.kt>:
// test.kt:12 box:
// test.kt:4 <init>: x=1:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:12 box:
// test.kt:6 foo: a=A, block=Function3
// test.kt:13 box$lambda: w=1:number
// test.kt:1 component1:
// test.kt:13 box$lambda: w=1:number, x=1:number
// test.kt:1 component3:
// test.kt:15 box$lambda: w=1:number, x=1:number, y=48:number
// test.kt:4 <init>: x=1:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:15 box$lambda: w=1:number, x=1:number, y=48:number
// test.kt:1 component1:
// test.kt:15 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number
// test.kt:1 component3:
// test.kt:16 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number
// test.kt:4 <init>: x=1:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:4 <init>: x=1:number, y="":kotlin.String, z=48:number
// test.kt:16 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number
// test.kt:1 component2:
// test.kt:16 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String
// test.kt:1 component3:
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:8 <get-arrayOfA>:
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number
// test.kt:1 component2:
// test.kt:18 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number, q="":kotlin.String
// test.kt:22 box$lambda: w=1:number, x=1:number, y=48:number, a=1:number, c=48:number, _="":kotlin.String, d=48:number, q="":kotlin.String
// test.kt:24 box:
