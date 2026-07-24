
// WITH_STDLIB
// FILE: test.kt

private class Context1(val id: Int)
private class Context2(val id: Int)

private object Container {
    context(context1: Context1, context2: Context2)
    fun caller(value: Int) {
        target(value)
    }

    context(context1: Context1, context2: Context2)
    private fun target(value: Int) {
        println(context1.id + context2.id + value)
    }
}

fun box() {
    context(Context1(1), Context2(2)) {
        Container.caller(3)
    }
}

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:5 <init>
// test.kt:21 box
// test.kt:6 <init>
// test.kt:21 box
// test.kt:22 box
// test.kt:11 caller
// test.kt:16 target
// test.kt:5 getId
// test.kt:16 target
// test.kt:6 getId
// test.kt:16 target
// test.kt:17 target
// test.kt:12 caller
// test.kt:23 box
// test.kt:21 box
// test.kt:24 box
