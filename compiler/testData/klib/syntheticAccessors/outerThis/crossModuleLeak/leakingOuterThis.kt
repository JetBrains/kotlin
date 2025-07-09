// WITH_STDLIB

// MODULE: lib
// FILE: Outer.kt
@Suppress("NOTHING_TO_INLINE")
class Outer {
    val publicPropertyOfOuter = "publicPropertyOfOuter"
    private val privatePropertyOfOuter = "privatePropertyOfOuter"

    fun publicFunctionOfOuter() = "publicFunctionOfOuter"
    private fun privateFunctionOfOuter() = "privateFunctionOfOuter"

    inner class InnerL1 {
        inline fun usesPublicPropertyOfOuterInInnerL1() = publicPropertyOfOuter
        internal inline fun usesPrivatePropertyOfOuterInInnerL1() = privatePropertyOfOuter

        inline fun usesPublicFunctionOfOuterInInnerL1() = publicFunctionOfOuter()
        internal inline fun usesPrivateFunctionOfOuterInInnerL1() = privateFunctionOfOuter()

        val publicPropertyOfInnerL1 = "publicPropertyOfInnerL1"
        private val privatePropertyOfInnerL1 = "privatePropertyOfInnerL1"

        fun publicFunctionOfInnerL1() = "publicFunctionOfInnerL1"
        private fun privateFunctionOfInnerL1() = "privateFunctionOfInnerL1"

        inner class InnerL2 {
            inline fun usesPublicPropertyOfOuterInInnerL2() = publicPropertyOfOuter
            internal inline fun usesPrivatePropertyOfOuterInInnerL2() = privatePropertyOfOuter

            inline fun usesPublicFunctionOfOuterInInnerL2() = publicFunctionOfOuter()
            internal inline fun usesPrivateFunctionOfOuterInInnerL2() = privateFunctionOfOuter()

            inline fun usesPublicPropertyOfInnerL1InInnerL2() = publicPropertyOfInnerL1
            internal inline fun usesPrivatePropertyOfInnerL1InInnerL2() = privatePropertyOfInnerL1

            inline fun usesPublicFunctionOfInnerL1InInnerL2() = publicFunctionOfInnerL1()
            internal inline fun usesPrivateFunctionOfInnerL1InInnerL2() = privateFunctionOfInnerL1()
        }
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    val innerL1 = Outer().InnerL1()
    val innerL2 = innerL1.InnerL2()

    // Keep these variables here to have short access to their initializers with synthetic accessors:
    val usesPublicPropertyOfOuterInInnerL1 = innerL1.usesPublicPropertyOfOuterInInnerL1()
    val usesPrivatePropertyOfOuterInInnerL1 = innerL1.usesPrivatePropertyOfOuterInInnerL1()
    val usesPublicFunctionOfOuterInInnerL1 = innerL1.usesPublicFunctionOfOuterInInnerL1()
    val usesPrivateFunctionOfOuterInInnerL1 = innerL1.usesPrivateFunctionOfOuterInInnerL1()

    val usesPublicPropertyOfOuterInInnerL2 = innerL2.usesPublicPropertyOfOuterInInnerL2()
    val usesPrivatePropertyOfOuterInInnerL2 = innerL2.usesPrivatePropertyOfOuterInInnerL2()
    val usesPublicFunctionOfOuterInInnerL2 = innerL2.usesPublicFunctionOfOuterInInnerL2()
    val usesPrivateFunctionOfOuterInInnerL2 = innerL2.usesPrivateFunctionOfOuterInInnerL2()

    val usesPublicPropertyOfInnerL1InInnerL2 = innerL2.usesPublicPropertyOfInnerL1InInnerL2()
    val usesPrivatePropertyOfInnerL1InInnerL2 = innerL2.usesPrivatePropertyOfInnerL1InInnerL2()
    val usesPublicFunctionOfInnerL1InInnerL2 =  innerL2.usesPublicFunctionOfInnerL1InInnerL2()
    val usesPrivateFunctionOfInnerL1InInnerL2 = innerL2.usesPrivateFunctionOfInnerL1InInnerL2()

    // And now check the values:
    var numberOfMismatches = 0
    val dump = StringBuilder()

    fun StringBuilder.checkAndDumpValue(actual: String, name: String, expected: String): Int {
        return if (expected == actual) {
            appendLine("$name: $actual")
            0
        } else {
            appendLine("$name: $expected $actual")
            1
        }
    }

    numberOfMismatches += dump.checkAndDumpValue(usesPublicPropertyOfOuterInInnerL1, "usesPublicPropertyOfOuterInInnerL1", "publicPropertyOfOuter")
    numberOfMismatches += dump.checkAndDumpValue(usesPrivatePropertyOfOuterInInnerL1, "usesPrivatePropertyOfOuterInInnerL1", "privatePropertyOfOuter")
    numberOfMismatches += dump.checkAndDumpValue(usesPublicFunctionOfOuterInInnerL1, "usesPublicFunctionOfOuterInInnerL1", "publicFunctionOfOuter")
    numberOfMismatches += dump.checkAndDumpValue(usesPrivateFunctionOfOuterInInnerL1, "usesPrivateFunctionOfOuterInInnerL1", "privateFunctionOfOuter")

    numberOfMismatches += dump.checkAndDumpValue(usesPublicPropertyOfOuterInInnerL2, "usesPublicPropertyOfOuterInInnerL2", "publicPropertyOfOuter")
    numberOfMismatches += dump.checkAndDumpValue(usesPrivatePropertyOfOuterInInnerL2, "usesPrivatePropertyOfOuterInInnerL2", "privatePropertyOfOuter")
    numberOfMismatches += dump.checkAndDumpValue(usesPublicFunctionOfOuterInInnerL2, "usesPublicFunctionOfOuterInInnerL2", "publicFunctionOfOuter")
    numberOfMismatches += dump.checkAndDumpValue(usesPrivateFunctionOfOuterInInnerL2, "usesPrivateFunctionOfOuterInInnerL2", "privateFunctionOfOuter")

    numberOfMismatches += dump.checkAndDumpValue(usesPublicPropertyOfInnerL1InInnerL2, "usesPublicPropertyOfInnerL1InInnerL2", "publicPropertyOfInnerL1")
    numberOfMismatches += dump.checkAndDumpValue(usesPrivatePropertyOfInnerL1InInnerL2, "usesPrivatePropertyOfInnerL1InInnerL2", "privatePropertyOfInnerL1")
    numberOfMismatches += dump.checkAndDumpValue(usesPublicFunctionOfInnerL1InInnerL2, "usesPublicFunctionOfInnerL1InInnerL2", "publicFunctionOfInnerL1")
    numberOfMismatches += dump.checkAndDumpValue(usesPrivateFunctionOfInnerL1InInnerL2, "usesPrivateFunctionOfInnerL1InInnerL2", "privateFunctionOfInnerL1")

    return if (numberOfMismatches > 0) dump.toString() else "OK"
}
