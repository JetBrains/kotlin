// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

annotation class Anno
annotation class AnnoWithArguments(val i: Int)

context(@Anno @AnnoWithArguments(0) para<caret>meter1: @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>)
val foo: Int get() = 0
