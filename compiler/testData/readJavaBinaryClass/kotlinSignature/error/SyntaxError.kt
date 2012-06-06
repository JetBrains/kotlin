package test

import java.util.*

public open class SyntaxError : Object() {
    open fun foo() : Int? {
        throw UnsupportedOperationException()
    }
}
