package foo

import bar.CrExtended

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