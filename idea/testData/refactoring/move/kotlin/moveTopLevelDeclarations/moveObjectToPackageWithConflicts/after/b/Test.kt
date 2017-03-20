package b

import a.Foo
import a.foo

private object Test {
    fun test {
        foo(Foo())
    }
}