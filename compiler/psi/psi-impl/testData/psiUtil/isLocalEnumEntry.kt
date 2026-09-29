enum class EnumClass {
    ENTRY {
        class NestedInEntryLocal {
            class NestedNestedInEntryLocal

            fun nestedMemberInEntryLocal() {}
        }

        inner class InnerInEntryLocal

        object ObjectInEntryLocal

        fun memberInEntryLocal() {
            class BodyLocal

            fun bodyFunctionLocal() {}
        }

        val propertyInEntryLocal = 1

        init {
            class InitLocal

            fun initFunctionLocal() {}

            val initPropertyLocal = 1
        }
    },
    PLAIN;

    class EnumNested

    fun enumMember() {}

    val enumProperty = 1
}
