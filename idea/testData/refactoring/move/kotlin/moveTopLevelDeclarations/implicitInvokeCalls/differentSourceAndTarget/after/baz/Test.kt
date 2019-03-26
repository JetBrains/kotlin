package baz

import bar.CrExtended
import foo.extValWithExtFunType
import foo.extValWithFunType
import foo.valWithExtFunType
import foo.valWithFunType

fun test(ce: CrExtended) {
    valWithFunType()
    ce.valWithExtFunType()
    with(1) {
        extValWithFunType()
        ce.extValWithExtFunType()
    }

    ::valWithFunType
    ::valWithExtFunType
    1::extValWithFunType
    1::extValWithExtFunType
}