fun test() {
    fun test1() {}
    fun test1() {}

    fun Any.test2() {}
    fun test2(x: Any) = x

    fun Any.test3() {}
    fun Any.test3() {}

    fun test4(): Int = 0
    fun test4(): String = ""

    class Test5(val x: Int) {
        constructor(): this(0)
    }
    fun Test5() {}
    fun Test5(x: Int) = x

    fun local() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x
    }
}

class Test {
    init {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x
    }

    fun test() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x
    }

    val property: Any get() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x

        return 0
    }
}

val property: Any get() {
    fun test1() {}
    fun test1() {}

    fun Any.test2() {}
    fun test2(x: Any) = x

    fun Any.test3() {}
    fun Any.test3() {}

    fun test4(): Int = 0
    fun test4(): String = ""

    class Test5(val x: Int) {
        constructor(): this(0)
    }
    fun Test5() {}
    fun Test5(x: Int) = x

    return 0
}

object Object {
    fun test() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x
    }

    val property: Any get() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x

        return 0
    }
}

val obj = object {
    fun test() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x
    }

    val property: Any get() {
        fun test1() {}
        fun test1() {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        fun Any.test3() {}
        fun Any.test3() {}

        fun test4(): Int = 0
        fun test4(): String = ""

        class Test5(val x: Int) {
            constructor(): this(0)
        }
        fun Test5() {}
        fun Test5(x: Int) = x

        return 0
    }
}