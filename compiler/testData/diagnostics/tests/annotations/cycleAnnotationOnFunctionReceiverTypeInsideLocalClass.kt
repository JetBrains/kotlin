// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

package myPack

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        fun @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>42.function()<!>) Int.function() = 0
    }
}
