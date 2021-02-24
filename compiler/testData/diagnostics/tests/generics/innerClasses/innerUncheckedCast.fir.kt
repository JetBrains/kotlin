// !WITH_NEW_INFERENCE
// !CHECK_TYPE

class Outer<E> {
    inner open class InnerBase<F>
    inner class Inner<H> : InnerBase<H>() {
        val prop: E = null!!
    }

    fun foo(x: InnerBase<String>, y: Any?, z: Outer<*>.InnerBase<String>) {
        if (x is Inner) {
            x.prop.checkType { _<E>() }
        }

        if (y is <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>) return

        if (z is Inner) {
            z.prop.checkType { _<Any?>() }
            return
        }

        if (y is Outer<*>.Inner<*>) {
            y.prop.checkType { _<Any?>() }
        }
    }

    fun bar(x: InnerBase<String>, y: Any?, z: Outer<*>.InnerBase<String>) {
        x as Inner
        y as <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>
        z as Inner
    }
}
