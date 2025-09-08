// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val s: String)

fun f1() {
    class Foo {
        context(@Anno("par") pa<caret>r: @Anno("foo") String)
        fun bar(): String = "hello"
    }
}
