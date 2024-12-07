// RUN_PIPELINE_TILL: FRONTEND
package myPack

@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)


fun topLevel() {
    class LocalClass {
        val @Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>42.property<!>) Int.property get() = 0
    }
}
