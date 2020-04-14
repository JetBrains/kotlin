// FILE: test.kt
class Foo {
    inner class Bar {
    }
}

fun box() {
    val x = Foo()
    x.Bar()
}

// From receiverMangling Test:
// public final class Foo$Bar : java/lang/Object {
//    final Foo this$0
//
//    public void <init>(Foo this$0)
//}
//
//public final class Foo : java/lang/Object {
//    public void <init>()
//}

// TODO: Capture `this$0` naming on Foo$Bar:3:

// LOCAL VARIABLES
// TestKt:8:
// Foo:2:
// TestKt:8:
// TestKt:9: x:Foo
// Foo$Bar:3:
// TestKt:9: x:Foo
// TestKt:10: x:Foo