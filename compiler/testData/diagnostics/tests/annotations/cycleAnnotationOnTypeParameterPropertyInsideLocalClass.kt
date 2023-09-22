// FIR_IDENTICAL
package myPack

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val number: Int)

fun topLevelFun() {
    class LocalClass {
        val <@Anno(42.<!UNRESOLVED_REFERENCE!>prop<!>) T> T.prop get() = 22
    }
}
