package test

import java.util.*

public open class MethodWithMappedClasses : Object() {
    public open fun <T> copy(dest : MutableList<in T>, src : List<T>) {}
}
