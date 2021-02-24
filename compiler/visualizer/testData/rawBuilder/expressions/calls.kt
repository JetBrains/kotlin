// FIR_IGNORE
// WITH_RUNTIME
//                                 Int
//                                 │ distance.x: Int
//                                 │ │ fun (Int).plus(Int): Int
//                                 │ │ │ distance.y: Int
//                                 │ │ │ │
infix fun distance(x: Int, y: Int) = x + y

//              [ERROR: unknown type]
//              │ Int
//              │ │ [ERROR: not resolved]
//              │ │ │        Int
//              │ │ │        │
fun test(): Int = 3 distance 4

//                     Int
//                     │ fun distance(Int, Int): Int
//                     │ │        Int
//                     │ │        │  Int
//                     │ │        │  │
fun testRegular(): Int = distance(3, 4)

class My(var x: Int) {
//                        Int
//                        │ var (My).x: Int
//                        │ │
    operator fun invoke() = x

    fun foo() {}

//             My
//             │ constructor My(Int)
//             │ │  var (My).x: Int
//             │ │  │
    fun copy() = My(x)
}

//                    Int
//                    │ constructor My(Int)
//                    │ fun (My).invoke(): Int
//                    │ │  Int
//                    │ │  │
fun testInvoke(): Int = My(13)()

fun testQualified(first: My, second: My?) {
//  fun io/println(Int): Unit
//  │       testQualified.first: My
//  │       │     var (My).x: Int
//  │       │     │
    println(first.x)
//  fun io/println(Any?): Unit
//  │       testQualified.second: My?
//  │       │       var (My).x: Int
//  │       │       │
    println(second?.x)
//  testQualified.first: My
//  │     fun (My).foo(): Unit
//  │     │
    first.foo()
//  testQualified.second: My?
//  │       fun (My).foo(): Unit
//  │       │
    second?.foo()
//  testQualified.first: My
//  │     fun (My).copy(): My
//  │     │      fun (My).foo(): Unit
//  │     │      │
    first.copy().foo()
//  testQualified.first: My
//  │     var (My).x: Int
//  │     │   Int
//  │     │   │
    first.x = 42
}
