package test

import java.util.*

public open class MethodWithMappedClasses {
    public open fun <T> copy(dest : MutableList<in T>, src : List<T>) {}
}
