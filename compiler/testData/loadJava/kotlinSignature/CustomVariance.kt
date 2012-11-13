package test

import java.util.*

public open class CustomVariance : Object() {
    public open fun foo() : MutableList<out Number> = throw UnsupportedOperationException()
}
