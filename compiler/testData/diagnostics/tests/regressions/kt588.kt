// KT-588 Unresolved static method

class Test() : Thread("Test") {
    class object {
        fun init2() {

        }
    }
    override fun run() {
        init2()      // unresolved
        Test.init2() // ok
    }
}
