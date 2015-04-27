// KT-439 Support labeled function literals in call arguments

inline fun run1<T>(body : () -> T) : T = body()

fun main1(<!UNUSED_PARAMETER!>args<!> : Array<String>) {
    run1 l@{ 1 } // should not be an error
}
