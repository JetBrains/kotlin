// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// TARGET_PLATFORM: JVM

@JvmInline
value class ValueClass(val value: String) {
    companion {
        fun companionBlockFunction() {}
        fun companionBlockFunctionWithValueClassType(): ValueClass? = null

        @JvmOverloads
        fun companionBlockFunctionWithJvmOverloads(a: Int, b: Int = 0) {}

        val companionBlockProperty: Int get() = 0
        var companionBlockVariable: Int = 0
        var companionBlockVariableWithValueClassType: ValueClass? = null
    }
}
