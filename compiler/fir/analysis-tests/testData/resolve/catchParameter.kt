fun <T> test() {
    try {

    } catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: NullPointerException = NullPointerException()<!>) {

    }

    try {} catch (<!TYPE_PARAMETER_IN_CATCH_CLAUSE!>e: T<!>) {}
}

inline fun <reified T> anotherTest() {
    try {} catch (<!REIFIED_TYPE_IN_CATCH_CLAUSE!>e: T<!>) {}
}