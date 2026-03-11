
enum class MyEnumClass {
    FirstEntry {
        val a: Int = 1
    },
    SecondEntry,
    ThirdEntry {
        val b = 2
        val Int.d get() = 2
    }
}
