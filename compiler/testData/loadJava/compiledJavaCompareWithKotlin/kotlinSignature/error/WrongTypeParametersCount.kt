package test

import java.util.*

public open class WrongTypeParametersCount {
    public open fun <A, B> foo(p0 : A?, p1 : List<B>?) {
    }
}
