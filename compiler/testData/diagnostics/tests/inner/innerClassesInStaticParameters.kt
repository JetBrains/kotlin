// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
class Test {
    companion object {
        fun test(t: TestInner) = 42
    }

    class TestStatic {
        fun test(t: TestInner) = 42
    }

    inner class TestInner
}