package testing

fun testTop() {

}

class TestSample() {
    fun main(args : Array<String>) {
        val testVar = ""
        test<caret>.testFun()
    }

    fun testFun() {

    }
}

// INVOCATION_COUNT: 2
// EXIST: testVar, testFun, testTop