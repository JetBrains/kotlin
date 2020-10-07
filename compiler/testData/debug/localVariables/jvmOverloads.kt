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
// TestKt:9:
// C:3:
// TestKt:9:
// C:4:
// C:5: firstParam:int=4:int, secondParam:java.lang.String="":java.lang.String
// C:4:
// TestKt:10: