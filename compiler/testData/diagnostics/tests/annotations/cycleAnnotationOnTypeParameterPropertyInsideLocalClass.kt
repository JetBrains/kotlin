// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        val <@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>42.prop<!>) T> T.prop get() = 22
    }
}
