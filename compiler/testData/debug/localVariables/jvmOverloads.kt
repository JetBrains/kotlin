// FILE: test.kt
// WITH_RUNTIME
class C {
    @kotlin.jvm.JvmOverloads fun foo(firstParam: Int, secondParam: String = "") {
    }
}

fun box() {
    C().foo(4)
}

// LOCAL VARIABLES
// test.kt:9 box:
// test.kt:3 <init>:
// test.kt:9 box:
// test.kt:5 foo: firstParam:int=4:int, secondParam:java.lang.String="":java.lang.String
// test.kt:10 box: