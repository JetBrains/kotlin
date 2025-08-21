package foo

annotation class Anno(val i: Int)
const val CONSTANT = 0

enum class MyEnumClass {
    Entry {
        @Anno(<expr>CONSTANT</expr>)
    }
}
