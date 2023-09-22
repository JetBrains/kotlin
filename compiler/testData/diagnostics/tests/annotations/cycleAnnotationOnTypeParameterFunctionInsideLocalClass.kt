// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        fun <@Anno(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>function<String>()<!>) T> function() = 1
    }
}
