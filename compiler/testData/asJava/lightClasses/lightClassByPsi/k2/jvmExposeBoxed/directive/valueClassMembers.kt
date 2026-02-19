// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package pack

class OriginalClass

@JvmInline
value class ValueClass(val original: OriginalClass) {
    fun funWithoutParameters() {}
    fun funWithSelfParameter(v: ValueClass) {}
    val property: Int get() = 4
    val propertyWithValueClassType: ValueClass get() = this

    object RegularObject {}

    companion object {
        val companionPropertyWithValueClassType: ValueClass? = null
        val companionProperty: Int = 0

        fun companionFunction() {}
        fun companionFunctionWithValueClassType(): ValueClass? = null
    }
}
// LIGHT_ELEMENTS_NO_DECLARATION: ValueClass.class[companionFunctionWithValueClassType-RcbxKLE;constructor-impl;equals-impl;equals-impl0;funWithSelfParameter-0JCZ7rA;funWithoutParameters-impl;getCompanionPropertyWithValueClassType-RcbxKLE;getProperty-impl;getPropertyWithValueClassType-wCez43g;hashCode-impl;toString-impl]