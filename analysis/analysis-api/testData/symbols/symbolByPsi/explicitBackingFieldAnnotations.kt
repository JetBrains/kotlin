// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// WITH_STDLIB

@Target(AnnotationTarget.FIELD)
annotation class Anno

class Test {
    val foo: String
        @Anno field = ""
}