package test

import java.util.*

public open class CustomProjectionKind : Object() {
    public open fun foo() : MutableList<out Number> = throw UnsupportedOperationException()
}
