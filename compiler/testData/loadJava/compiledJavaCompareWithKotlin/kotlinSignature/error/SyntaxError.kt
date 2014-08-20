package test

import java.util.*

public open class SyntaxError {
    public open fun foo() : Int? {
        throw UnsupportedOperationException()
    }
}
