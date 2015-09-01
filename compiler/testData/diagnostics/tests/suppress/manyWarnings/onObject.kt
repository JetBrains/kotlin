@Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
object C {
    fun foo(): String?? = ""!! <!USELESS_CAST!>as String??<!>
}