open class BaseClass

class Child : BaseClass() {
    override fun equals(other: Any?): Boolean = true
}

fun myClass(b1: BaseClass, b2: BaseClass) {
    if (b1 is Child) {
        b1 =<caret>= b2
    }
}
