package test

import java.util.*

public open class WrongValueParametersCount : Object() {
    public open fun foo() : Int? {
        throw UnsupportedOperationException()
    }
}
