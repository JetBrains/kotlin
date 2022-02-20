package com.example

@Suppress("NOTHING_TO_INLINE")
class SomeClass {

    // Constants are not allowed in a class, only allowed at the top level or in an object.

    companion object CompanionObject {
        const val constantChangedType: Long = 0
        const val constantChangedValue: Int = 1000
        const val constantUnchanged: Int = 0
        private const val privateConstantChangedType: Long = 0
    }

    inline fun inlineFunctionChangedSignature(): Long = 0
    inline fun inlineFunctionChangedImplementation(): Int = 1000
    inline fun inlineFunctionChangedUnchanged(): Int = 0
    private inline fun privateInlineFunctionChangedSignature(): Long = 0
}