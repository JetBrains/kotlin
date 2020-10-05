// FILE: test.kt
fun box() {
    try {
        var a = 1
        a --
        a /= a
    } catch(e : Throwable) {
        e.printStackTrace()
    }
}

// LOCAL VARIABLES
// TestKt:3:
// TestKt:4:
// TestKt:5: a:int=1:int
// TestKt:6: a:int=0:int
// TestKt:7:
// TestKt:8: e:java.lang.Throwable=java.lang.ArithmeticException
// TestKt:10:
