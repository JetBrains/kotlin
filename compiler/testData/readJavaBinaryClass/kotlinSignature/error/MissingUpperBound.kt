package test

import java.util.*

public open class MissingUpperBound : Object() {
    public open fun <A : Runnable?> foo() : String? where A : Cloneable? {
        throw UnsupportedOperationException()
    }
}
