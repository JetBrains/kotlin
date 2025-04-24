// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77137

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

abstract class Super(c: Int)

class TopLevelClass() {
    @Anno(inner)
    companion object : @Anno(inner) Super(<!UNRESOLVED_REFERENCE!>inner<!>) {
        const val inner = 1
    }
}
