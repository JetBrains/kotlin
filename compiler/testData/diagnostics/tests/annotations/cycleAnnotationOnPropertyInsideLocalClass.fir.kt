package myPack

annotation class Anno(val number: Int)

fun topLevel() {
    class LocalClass {
        @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>prop<!>)
        var prop
            @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>prop<!>)
            get() = 22
            @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>prop<!>)
            set(@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>prop<!>) value) = Unit
    }
}
