package b

import a.Foo
import a.foo

private var test: String
    get() = ""
    set(value: String) {
        foo(Foo())
    }