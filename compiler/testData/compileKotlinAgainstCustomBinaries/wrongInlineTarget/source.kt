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

    val base = Base()
    base.inlineFunBase {}
    base.inlineGetterBase
    base.inlineGetterBase = 1

    base.inlineSetterBase
    base.inlineSetterBase = 1

    base.allInlineBase
    base.allInlineBase = 1
}


class Derived : Base() {

    fun test() {
        inlineFunBase {}
        inlineGetterBase
        inlineGetterBase = 1

        inlineSetterBase
        inlineSetterBase = 1

        allInlineBase
        allInlineBase = 1
    }
}
