// ISSUE: KT-70756

operator fun <T> String.invoke(t: T) {}

fun main() {
    <!FUNCTION_EXPECTED!>8<!> {
        {
            1
        }
    }
}