// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// WITH_STDLIB

fun throwException(): Nothing = throw RuntimeException()

fun test(){
    throwException()
    <!UNREACHABLE_CODE!>val b = object {
        val a = 0
        val b = 0
    }<!>
}

fun test2() {
    throwException()
    <!UNREACHABLE_CODE!>class LocalClass {
        val a = 0
        val b = 0
    }<!>
}

fun test3() {
    throwException()
    <!UNREACHABLE_CODE!>fun local() {
        val a = 0
        val b = 0
    }<!>
}

fun test4() {
    throwException()
    <!UNREACHABLE_CODE, UNUSED_EXPRESSION!>fun() {
        val a = 0
        val b = 0
    }<!>
}

fun test5() {
    throwException()
    <!UNREACHABLE_CODE!>val a = { c: Int, b: Int -> c + b }<!>
}

fun test6() {
    throwException()
    <!UNREACHABLE_CODE!>val a: String by <!UNREACHABLE_CODE!>lazy {
        val a = "0"
        val b = "0"
        a
    }<!><!>
}

fun test7(){
    throwException()
    <!UNREACHABLE_CODE!>class Local {
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
        val d = object {
            val a = 0
            val b = 0
        }
    }<!>
}

fun test8() {
    throwException()
    <!UNREACHABLE_CODE!>fun local(){
        val a = fun() {
            val e = 0
            val f = 0
        }
        val b: Int by <!UNREACHABLE_CODE!>lazy {
            val e = 0
            val f = 0
            e
        }<!>
        val c = {
            val e = 0
            val f = 0
        }
        val d = object {
            val e = 0
            val f = 0
        }
    }<!>
}

fun test9() {
    throwException()
    <!UNREACHABLE_CODE!>val k = object {
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

fun test10() {
    throwException()
    <!UNREACHABLE_CODE!>val k = {
        fun() {
            val c = 0
            val d = 0
        }
    }<!>
}

fun test11() {
    throwException()
    <!UNREACHABLE_CODE!>val k = {
        class A {
            val a = 0
            val b = 0
        }
    }<!>
}

fun test12(){
    throwException()
    <!UNREACHABLE_CODE!>val a = object {
        val b = {
            class A
        }
    }<!>
}