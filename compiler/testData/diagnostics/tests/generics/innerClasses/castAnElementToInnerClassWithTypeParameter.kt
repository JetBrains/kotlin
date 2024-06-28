// FIR_IDENTICAL
// ISSUE: KT-60921

abstract class A<T> {
    inner class B {
        inner class C

        fun f2(y: Any) {
            if (y is B<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>.C) { }

            if (y is B.C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>) { }

            if (y is B<*>.C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>) { }
        }
    }

    fun f1(x: Any) {
        if (x is B<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>) { }
    }
}
