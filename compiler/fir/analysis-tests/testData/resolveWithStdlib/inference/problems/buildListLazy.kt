data class NameAndSafeValue(val name: String, val value: Int)

fun getEnv() = listOf<NameAndSafeValue>()

private val environment: List<NameAndSafeValue> by lazy {
    buildList {
        getEnv().forEach { (name, value) ->
            this += NameAndSafeValue(name, value)
        }
        sortBy { <!ARGUMENT_TYPE_MISMATCH!>it.<!UNRESOLVED_REFERENCE!>name<!><!> }
    }
}
