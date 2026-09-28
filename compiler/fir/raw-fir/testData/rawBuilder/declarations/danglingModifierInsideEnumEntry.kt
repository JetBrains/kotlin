package foo

annotation class Anno(val i: Int)

const val CONSTANT = 1

enum class MyEnumClass {
    Entry {
        @Anno(CONSTANT)
    }
}
