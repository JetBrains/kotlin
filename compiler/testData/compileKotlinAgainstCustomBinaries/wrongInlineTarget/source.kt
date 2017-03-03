package usage

import a.*

fun baz() {
    inlineFun {}
    inlineGetter
    inlineGetter = 1

    inlineSetter
    inlineSetter = 1

    allInline
    allInline = 1

    val a = A()
    a.inlineFun {}
    a.inlineGetter
    a.inlineGetter = 1

    a.inlineSetter
    a.inlineSetter = 1

    a.allInline
    a.allInline = 1
}


