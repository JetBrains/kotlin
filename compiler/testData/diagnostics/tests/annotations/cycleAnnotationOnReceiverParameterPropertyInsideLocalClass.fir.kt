package myPack

annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        val @receiver:Anno(42.<!UNRESOLVED_REFERENCE!>prop<!>) Int.prop get() = 22
    }
}
