package test

import java.util.*

public open class WrongTypeVariance : Object() {
    public open fun copy(p0 : List<out java.lang.Number?>?, p1 : List<in java.lang.Number?>?) : List<java.lang.Number?>? {
        throw UnsupportedOperationException()
    }
}
