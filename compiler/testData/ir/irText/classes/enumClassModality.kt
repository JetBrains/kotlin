enum class TestFinalEnum1 {
    X1
}

enum class TestFinalEnum2(val x: Int) {
    X1(1)
}

enum class TestFinalEnum3 {
    X1
    ;

    fun doStuff() {}
}

enum class TestOpenEnum1 {
    X1 {
        override fun toString() = "X1"
    }
}

enum class TestOpenEnum2 {
    X1 {
        override fun foo() {}
    };

    open fun foo() {}
}

enum class TestAbstractEnum1 {
    X1 {
        override fun foo() {}
    };

    abstract fun foo()
}

interface IFoo {
    fun foo()
}

enum class TestAbstractEnum2 : IFoo {
    X1 {
        override fun foo() {}
    }
}
