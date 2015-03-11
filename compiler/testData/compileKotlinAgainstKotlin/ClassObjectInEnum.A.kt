package library

public enum class EnumClass {
    ENTRY

    public default object {
        public fun entry(): EnumClass = ENTRY
    }
}
