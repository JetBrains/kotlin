// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// WITH_STDLIB

fun throwException(): Nothing = throw RuntimeException()

class Test1 {
    val x: String = throwException()
    class Nested {
        val a = 0
        val b = 0
    }
}

class Test2 {
    val x: String = throwException()
    val o = <!UNREACHABLE_CODE!>{
        val a = 0
        val b = 0
    }<!>
}

class Test3 {
    val x: String = throwException()
    val o: String by <!UNREACHABLE_CODE!>lazy {
        val a = "a"
        val b = 0
        a
    }<!>
}

class Test4 {
    val x: String = throwException()
    init {
        <!UNREACHABLE_CODE!>val a = 0<!>
        <!UNREACHABLE_CODE!>val b = 0<!>
    }
}

class Test5 {
    val x: String = throwException()
    constructor(a: Int)<!UNREACHABLE_CODE!><!> {
        <!UNREACHABLE_CODE!>val c = 0<!>
        <!UNREACHABLE_CODE!>val b = 0<!>
    }
}

class Test6 {
    val x: String = throwException()
    val o = <!UNREACHABLE_CODE!>fun() {
        val a = 0
        val b = 0
    }<!>
}

class Test7 {
    val x: String = throwException()
    val o = <!UNREACHABLE_CODE!>object {
        init {
            val a = 0
            val b = 0
        }
        val a = fun() {
            val a = 0
            val b = 0
        }
        val b: Int by <!UNREACHABLE_CODE!>lazy {
            val a = 0
            val b = 0
            b
        }<!>
        val c = {
            val a = 0
            val b = 0
        }
    }<!>
}

class Test8 {
    val x: String = throwException()
    class Nested {
        init {
            val a = 0
            val b = 0
        }
        val a = fun() {
            val a = 0
            val b = 0
        }
        val b: Int by lazy {
            val a = 0
            val b = 0
            b
        }
        val c = {
            val a = 0
            val b = 0
        }
        val d = object {
            val a = 0
            val b = 0
        }
    }
}

class Test9 {
    val x: String = throwException()
    fun foo() {
        val a = fun() {
            val e = 0
            val f = 0
        }
        val b: Int by lazy {
            val e = 0
            val f = 0
            e
        }
        val c = {
            val e = 0
            val f = 0
        }
        val d = object {
            val e = 0
            val f = 0
        }
    }
}

class Test10 {
    val x: String = throwException()
    init {
        <!UNREACHABLE_CODE!>class A<!>
    }
    constructor(i: Int)<!UNREACHABLE_CODE!><!>{
        <!UNREACHABLE_CODE!>class B<!>
    }
    val a = <!UNREACHABLE_CODE!>{
        class C
    }<!>

    val b = <!UNREACHABLE_CODE!>fun(){
        class D
    }<!>

    val c: Int by <!UNREACHABLE_CODE!>lazy {
        class E
        1
    }<!>
}

class Test11 {
    val x: String = throwException()

    val a = <!UNREACHABLE_CODE!>object {
        val b = {
            class A
        }
    }<!>
}