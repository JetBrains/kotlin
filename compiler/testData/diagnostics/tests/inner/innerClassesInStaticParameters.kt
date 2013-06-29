class Test {
    class object {
        fun test(<!UNUSED_PARAMETER!>t<!>: TestInner) = 42
    }

    class TestStatic {
        fun test(<!UNUSED_PARAMETER!>t<!>: TestInner) = 42
    }

    inner class TestInner
}