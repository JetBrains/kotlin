// FIR_IDENTICAL
// LANGUAGE: +ProhibitArrayLiteralsInCompanionOfAnnotation
// ISSUE: KT-39041

annotation class Ann(val x: IntArray = [1, 2, 3]) { // OK
    companion object {
        val y1: IntArray = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

        val z1: IntArray
            get() = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

        fun test_1(): IntArray {
            return <!UNSUPPORTED!>[1, 2, 3]<!> // Error
        }

        class Nested {
            val y2: IntArray = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

            val z2: IntArray
                get() = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

            fun test_2(): IntArray {
                return <!UNSUPPORTED!>[1, 2, 3]<!> // Error
            }
        }
    }

    object Foo {
        val y3: IntArray = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

        val z3: IntArray
            get() = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

        fun test_3(): IntArray {
            return <!UNSUPPORTED!>[1, 2, 3]<!> // Error
        }
    }

    class Nested {
        val y4: IntArray = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

        val z4: IntArray
            get() = <!UNSUPPORTED!>[1, 2, 3]<!> // Error

        fun test_4(): IntArray {
            return <!UNSUPPORTED!>[1, 2, 3]<!> // Error
        }
    }
}
