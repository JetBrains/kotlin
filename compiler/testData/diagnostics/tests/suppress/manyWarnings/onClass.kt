@Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
class C {
    fun foo(): String?? = ""!! <!USELESS_CAST!>as String??<!>
}