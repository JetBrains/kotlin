// !LANGUAGE: +NewInference
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.unit -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Check of Unit type
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1

fun foo() {
    val proc = "case 1"
}

class Case1() {
    checkType<Unit>(foo())
}

// TESTCASE NUMBER: 2

fun case2foo(m: String, bar: (m: String) -> Unit) {
    bar(m)
}


class Case2Boo {
    fun buz(m: String) {
        val proc = "case 2"
    }
}


class Case2() {
    val boo = Case2Boo()
    val res = case2foo("s", boo::buz)
    checkType<Unit>(res)
}

// TESTCASE NUMBER: 3
interface Processable<T> {
    fun process(): T
}

class Processor : Processable<Unit> {
    override fun process() {
        val proc = "case 3"

    }
}

class Case3() {
    val p1 = Processor().process()
    checkType<Unit>(p1)

}

// TESTCASE NUMBER: 4

class Case4() {
    val p2 = object : Processable<Unit> {
        override fun process() {
            val proc = "case 4"
        }
    }
    checkType<Unit>(p2)
}