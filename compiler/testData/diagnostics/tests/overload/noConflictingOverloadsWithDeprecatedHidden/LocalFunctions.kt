// FIR_IDENTICAL

fun test() {
    <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
    fun test1()<!> {}

    fun Any.test2() {}
    fun test2(x: Any) = x

    <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
    fun Any.test3()<!> {}

    <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
    <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
    fun test4(): String<!> = ""

    class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
        <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
    }
    <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
    fun Test5()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
    fun Test5(x: Int)<!> = x

    fun local() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x
    }
}

class Test {
    init {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x
    }

    fun test() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x
    }

    val property: Any get() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x

        return 0
    }
}

val property: Any get() {
    <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
    fun test1()<!> {}

    fun Any.test2() {}
    fun test2(x: Any) = x

    <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
    fun Any.test3()<!> {}

    <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
    <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
    fun test4(): String<!> = ""

    class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
        <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
    }
    <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
    fun Test5()<!> {}
    <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
    fun Test5(x: Int)<!> = x

    return 0
}

object Object {
    fun test() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x
    }

    val property: Any get() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x

        return 0
    }
}

val obj = object {
    fun test() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x
    }

    val property: Any get() {
        <!CONFLICTING_OVERLOADS!>fun test1()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test1", level = DeprecationLevel.HIDDEN)
        fun test1()<!> {}

        fun Any.test2() {}
        fun test2(x: Any) = x

        <!CONFLICTING_OVERLOADS!>fun Any.test3()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("test3", level = DeprecationLevel.HIDDEN)
        fun Any.test3()<!> {}

        <!CONFLICTING_OVERLOADS!>fun test4(): Int<!> = 0
        <!CONFLICTING_OVERLOADS!>@Deprecated("test4", level = DeprecationLevel.HIDDEN)
        fun test4(): String<!> = ""

        class Test5<!CONFLICTING_OVERLOADS!>(val x: Int)<!> {
            <!CONFLICTING_OVERLOADS!>constructor()<!>: this(0)
        }
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5()<!> {}
        <!CONFLICTING_OVERLOADS!>@Deprecated("Test5", level = DeprecationLevel.HIDDEN)
        fun Test5(x: Int)<!> = x

        return 0
    }
}
