package myPack

annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        val @receiver:Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>42.prop<!>) Int.prop get() = 22
    }
}
