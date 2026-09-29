enum class EnumClass {
    ENTRY {
        class NestedInEntry {
            class NestedNestedInEntry

            fun nestedMemberInEntry() {}
        }

        inner class InnerInEntry

        object ObjectInEntry

        fun memberInEntry() {
            class BodyLocal

            fun bodyFunctionLocal() {}
        }

        val propertyInEntry = 1

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
