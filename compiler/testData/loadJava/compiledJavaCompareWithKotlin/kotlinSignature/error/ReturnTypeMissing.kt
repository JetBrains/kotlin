package test

import java.util.*

public open class ReturnTypeMissing {
    public open fun foo(p0 : String?) : Int {
        throw UnsupportedOperationException()
    }
}
