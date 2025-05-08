// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// IGNORE_FIR
// ^KT-76932

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

fun foo() {
    class Foo {
        context(para<caret>meter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
    }
}
