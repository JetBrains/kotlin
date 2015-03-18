// SIBLING:
class MyClass {
    fun test(): Int {
        <selection>coFun()
        return coProp + 10</selection>
    }

    companion object {
        val coProp = 1

        fun coFun() {

        }
    }
}
