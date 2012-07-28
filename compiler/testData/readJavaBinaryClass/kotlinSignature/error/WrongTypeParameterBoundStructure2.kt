package test

import java.util.*

public open class WrongTypeParameterBoundStructure2 : Object() {
    public open fun <erased A, erased B : Runnable?> foo(p0 : A?, p1 : List<out B>?, p2: List<in String?>?) where B : List<Cloneable?>? {
    }
}
