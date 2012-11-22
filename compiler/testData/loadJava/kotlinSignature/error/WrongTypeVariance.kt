package test

import java.util.*

public open class WrongTypeVariance : Object() {
    public open fun copy(p0 : List<out jet.Number?>?, p1 : List<out jet.Number?>?) : MutableList<jet.Number?>? {
        throw UnsupportedOperationException()
    }
}
