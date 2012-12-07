package test

import java.util.*

public open class WrongTypeParameterBoundStructure1 : Object() {
    public open fun <erased A, erased B : Runnable?> foo(p0 : A?, p1 : List<B>?) where B : List<Cloneable>? {
    }
}
