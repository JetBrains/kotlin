package test

import java.util.*

public open class SyntaxError : Object() {
    public open fun foo() : Int? {
        throw UnsupportedOperationException()
    }
}
