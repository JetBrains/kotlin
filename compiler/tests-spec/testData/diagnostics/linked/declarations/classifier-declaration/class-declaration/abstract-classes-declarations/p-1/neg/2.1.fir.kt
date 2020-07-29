// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

// TESTCASE NUMBER: 1
abstract class A1

fun case1() {
    val a = A1()
}

// TESTCASE NUMBER: 2
fun case2() {
    abstract class A1

    val a = A1()
}


// TESTCASE NUMBER: 3
class case3(val x: Int) {
    abstract inner class A1
    val a = A1()

    companion object{
        val a = A1()
    }
}


// TESTCASE NUMBER: 4
class Case4() {
    abstract class A1

    fun test() {
        val a = A1()
    }

    companion object{
        val a = A1()
    }
}
