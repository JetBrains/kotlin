class C {
    companion {
        fun foo() {}
    }

    fun member1() {}

    init {}

    companion {
        val bar = 1
        val baz get() = 2
    }

    val member2 = 1

    companion {
        fun qux() {
            class Local {
                fun insideLocal() {}
                val insideLocalVal = 1
            }
        }
    }

    companion object;

    companion {
        class Nested

        typealias TA = Int

        constructor() {}

        init {}

        companion {
            fun doubleNestedFun() {}

            val doubleNestedProp = 1

            class DoubleNestedNested

            typealias DoubleNestedTA = Int

            constructor(x: Int) {}

            init {}
        }
    }
}