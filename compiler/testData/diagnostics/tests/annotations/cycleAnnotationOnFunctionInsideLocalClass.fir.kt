package myPack

annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        @Anno(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>function(42)<!>)
        fun function(@Anno(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>function(24)<!>) param: Int = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>function(0)<!>) = 1
    }
}
