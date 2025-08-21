// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class Bar {
    context(a: Int)
    operator fun invoke(): String {
        return "context(Int) Bar.invoke "
    }
}

context(a: String)
operator fun Bar.invoke(): String {
    return "context(String) Bar.invoke "
}

class Foo {
    val foo = Bar()

    fun test(): String {
        fun foo(): String {
            return "local foo "
        }

        return with("") { foo() } +     //resolves to local foo
                with(1) { foo() }       //resolves to local foo
    }

    fun test2(): String {
        fun String.foo(): String {
            return "local foo "
        }

        return with("") { foo() } +         //resolves to local foo
                with(1) { foo() }           //resolves to context(Int) Bar.invoke
    }

    fun test3(): String {
        context(a: String) fun foo(): String {
            return "local foo "
        }

        return with("") { foo() } +     //resolves to local foo
                with(1) { foo() }       //resolves to context(Int) Bar.invoke
    }

    fun test4(): String {
        return with("") { foo() }              //resolves to context(String) Bar.invoke
    }
}

fun box(): String {
    var result = "OK"
    if (Foo().test() != "local foo local foo ") result = "not OK"
    if (Foo().test2() != "local foo context(Int) Bar.invoke ") result = "not OK"
    if (Foo().test3() != "local foo context(Int) Bar.invoke ") result = "not OK"
    if (Foo().test4() != "context(String) Bar.invoke ") result = "not OK"
    return result
}