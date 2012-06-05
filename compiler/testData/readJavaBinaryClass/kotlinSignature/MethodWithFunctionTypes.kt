package test

import java.util.*

public open class MethodWithFunctionTypes : Object() {
    open fun foo(p0 : (String?) -> String) : (String.() -> String?)? {
        throw UnsupportedOperationException()
    }
}
