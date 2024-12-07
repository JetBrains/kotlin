// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Outer {
    inner class Test1
    inner class Test2(val x: Int)
    inner class Test3(val x: Any)
    inner class Test4<T>(val x: T)
    inner class Test5(val x: Int) {
        constructor() : this(0)
        private constructor(z: String) : this(z.length)
    }

    class TestNested

    internal class TestInternal
    protected class TestProtected
    private class TestPrivate
}

fun Outer.Test1() {}
fun Outer.Test2(x: Int) {}
fun Outer.Test3(x: String) {}
fun <T> Outer.Test3(x: T) {}
fun <T : Number> Outer.Test4(x: T) {}
fun Outer.Test5() {}
fun Outer.Test5(z: String) {}

fun Outer.TestNested() {}
fun Outer.TestInternal() {}
fun Outer.TestProtected() {}
fun Outer.TestPrivate() {}