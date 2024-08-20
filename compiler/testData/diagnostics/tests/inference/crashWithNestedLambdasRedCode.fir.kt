// ISSUE: KT-70756

operator fun <T> String.invoke(t: T) {}

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>8<!> {
        {
            1
        }
    }<!>
}
