// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER

interface IBase {
    fun foo()
    fun bar()
    fun <T> qux(x: T)
}

class CDerived : IBase {
    override inline final fun foo() {}
    override inline fun bar() {}
    override inline final fun <reified T> qux(x: T) {}

    class CNested : IBase {
        override inline final fun foo() {}
        override inline fun bar() {}
        override inline final fun <reified T> qux(x: T) {}
    }

    val anObject = object : IBase {
        override inline final fun foo() {}
        override inline fun bar() {}
        override inline final fun <reified T> qux(x: T) {}
    }

    fun aMethod() {
        class CLocal : IBase {
            override inline final fun foo() {}
            override inline fun bar() {}
            override inline final fun <reified T> qux(x: T) {}
        }
    }
}

open class COpen : IBase {
    override inline final fun foo() {}
    override inline fun bar() {}
    override inline final fun <reified T> qux(x: T) {}

    open class COpenNested : IBase {
        override inline final fun foo() {}
        override inline fun bar() {}
        override inline final fun <reified T> qux(x: T) {}
    }

    val anObject = object : IBase {
        override inline final fun foo() {}
        override inline fun bar() {}
        override inline final fun <reified T> qux(x: T) {}
    }

    fun aMethod() {
        open class COpenLocal : IBase {
            override inline final fun foo() {}
            override inline fun bar() {}
            override inline final fun <reified T> qux(x: T) {}
        }
    }
}

