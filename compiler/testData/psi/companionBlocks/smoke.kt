// LANGUAGE: +CompanionBlocksAndExtensions
class C {
    companion {}
}

class C2 {
    companion {}
    companion {}
}

class C3 {
    fun foo() {}
    companion {
        fun bar() {}
    }
}

class C4 {
    fun foo() {}
    companion {
        fun bar() {}
    }
    companion {
        fun baz() {}
    }
}