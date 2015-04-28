suppress("REDUNDANT_NULLABLE")
class C {
    suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    fun foo(): String?? = ""!! <!USELESS_CAST!>as String??<!>
}