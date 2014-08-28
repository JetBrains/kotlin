package test

import java.util.*

public open class WrongProjectionKind {
    public open fun copy(p0 : Array<out Number>?, p1 : Array<out Number>?) : MutableList<Number>? {
        throw UnsupportedOperationException()
    }
}
