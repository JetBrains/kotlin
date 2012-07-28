package test

import java.util.*

public open class ReturnTypeMissing : Object() {
    public open fun foo(p0 : String?) : Int {
        throw UnsupportedOperationException()
    }
}
