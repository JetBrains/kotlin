package test

import java.util.*

public open class MethodWithFunctionTypes : Object() {
    public open fun foo(f : (String?) -> String) : (String.() -> String?)? {
        throw UnsupportedOperationException()
    }
}
