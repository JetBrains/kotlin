typealias StringType = String

fun <T : Throwable> test() {
    try {

    } catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: NullPointerException = NullPointerException()<!>) {

    }

    try {} catch (<!TYPE_PARAMETER_IN_CATCH_CLAUSE!>e: T<!>) {}

    try {} catch (<!TYPE_MISMATCH!>e: () -> Int<!>) {}

    try {} catch (<!TYPE_MISMATCH!>e: StringType<!>) {}

    try {} catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE, TYPE_MISMATCH!>e: Int = 5<!>) {}

    try {} catch (e: Throwable) {}
}

inline fun <reified T> anotherTest() {
    try {} catch (<!REIFIED_TYPE_IN_CATCH_CLAUSE, TYPE_MISMATCH!>e: T<!>) {}
}
