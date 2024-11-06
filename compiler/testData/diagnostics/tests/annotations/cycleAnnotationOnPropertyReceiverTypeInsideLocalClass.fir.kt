package myPack

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)


fun topLevel() {
    class LocalClass {
        val @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>42.property<!>) Int.property get() = 0
    }
}
