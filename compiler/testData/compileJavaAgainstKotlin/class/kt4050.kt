package test

annotation class AAA

enum class MyEnum(@param:AAA @property:Deprecated("") val ord: Int) {
    ENTRY(239);

    fun f(@java.lang.Deprecated p: Int) {

    }
}
