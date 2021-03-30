fun test() {
    try { } catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: Exception = Exception()<!>) { }
}
