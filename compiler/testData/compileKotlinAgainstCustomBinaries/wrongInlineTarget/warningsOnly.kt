package usage

import a.*

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
