enum class E {
    ENTRY;

    override val name: String = "lol"
    override val ordinal: Int = 0

    <!OVERRIDING_FINAL_MEMBER!>override<!> fun compareTo(other: E) = -1

    <!OVERRIDING_FINAL_MEMBER!>override<!> fun equals(other: Any?) = true
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun hashCode() = -1
}
