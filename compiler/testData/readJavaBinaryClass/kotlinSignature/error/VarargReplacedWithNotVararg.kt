package test

import java.util.*

public open class VarargReplacedWithNotVararg : Object() {
    public open fun foo(vararg p0 : String?) {
        throw UnsupportedOperationException()
    }
}
