// FILE: test.kt
class Foo {
    inner class Bar {
    }
}

fun box() {
    val x = Foo()
    x.Bar()
}

// LOCAL VARIABLES
// TestKt:8:
// Foo:2:
// TestKt:8:
// TestKt:9: LV:x:Foo
// Foo$Bar:3: F:this$0:null
// TestKt:9: LV:x:Foo
// TestKt:10: LV:x:Foo
