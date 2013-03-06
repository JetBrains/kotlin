package test

import java.util.*

public open class ExtraUpperBound : Object() {
    public open fun <A : Runnable?> foo() : String? {
        throw UnsupportedOperationException()
    }
}
