package foo

class A {
    fun test() {
        A.testCompanion()
        testCompanion()
        Companion.testCompanion()
    }

    companion object {
        fun testCompanion() {

        }
    }
}