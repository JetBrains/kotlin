// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE

@Target(AnnotationTarget.TYPE)
annotation class Anno
annotation class AnnoWithArguments(val i: Int)

fun foo() {
    val a = context(@Anno @AnnoWithArguments(0) para<caret>meter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>) fun() {

    }
}