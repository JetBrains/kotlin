package b

import a.Foo
import a.foo

private class Test {
    fun test {
        foo(Foo())
    }
}