package test

import java.util.*

public open class MethodWithTypeParameters : Object() {
    public open fun <erased A, erased B : Runnable> foo(p0 : A, p1 : List<B>, p2: MutableList<in String?>) where B : List<Cloneable> {
    }
}
