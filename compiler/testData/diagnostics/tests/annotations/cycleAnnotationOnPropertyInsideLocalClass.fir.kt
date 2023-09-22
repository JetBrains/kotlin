package myPack

annotation class Anno(val number: Int)

fun topLevel() {
    class LocalClass {
        @Anno(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>prop<!>)
        var prop
            @Anno(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>prop<!>)
            get() = 22
            @Anno(prop)
            set(@Anno(prop) value) = Unit
    }
}
