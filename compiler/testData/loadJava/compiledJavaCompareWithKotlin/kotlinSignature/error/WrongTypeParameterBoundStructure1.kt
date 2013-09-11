package test

import java.util.*

public open class WrongTypeParameterBoundStructure1 : Object() {
    public open fun <A, B : Runnable?> foo(p0 : A?, p1 : List<B>?) where B : List<Cloneable>? {
    }
}
