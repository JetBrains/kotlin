package test

enum class MyEnum(deprecated("") val ord: Int) {
    ENTRY: MyEnum(239)

    fun f(Deprecated p: Int) {

    }
}