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
    val o = {
        val a = 0
        val b = 0
    }
}

class Test3 {
    val x: String = throwException()
    val o: String by lazy {
        val a = "a"
        val b = 0
        a
    }
}

class Test4 {
    val x: String = throwException()
    init {
        val a = 0
        val b = 0
    }
}

class Test5 {
    <!UNREACHABLE_CODE!>val x: String =<!> throwException()
    constructor(a: Int) <!UNREACHABLE_CODE!>{
        val c = 0
        val b = 0
    }<!>
}

class Test6 {
    val x: String = throwException()
    val o = fun() {
        val a = 0
        val b = 0
    }
}

class Test7 {
    val x: String = throwException()
    val o = object {
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
    }
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
    <!UNREACHABLE_CODE!>val x: String =<!> throwException()
    init <!UNREACHABLE_CODE!>{
        class A
    }<!>
    constructor(i: Int)<!UNREACHABLE_CODE!>{
        class B
    }<!>
    <!UNREACHABLE_CODE!>val a = {
        class C
    }<!>

    <!UNREACHABLE_CODE!>val b = fun(){
        class D
    }<!>

    <!UNREACHABLE_CODE!>val c: Int by lazy {
        class E
        1
    }<!>
}

class Test11 {
    val x: String = throwException()

    val a = object {
        val b = {
            class A
        }
    }
}