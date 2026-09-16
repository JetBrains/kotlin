// pack.InlineClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

value class ValueClass(val first: Int, val second: Int)

@JvmInline
value class InlineClass(val value: ValueClass) {
    fun funWithValueParameter(v: ValueClass) {}
    fun funWithValueReturnType(): ValueClass = value
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: InlineClass.class[funWithValueParameter;funWithValueReturnType]
// LIGHT_ELEMENTS_NO_DECLARATION: InlineClass.class[constructor-impl;equals-impl;equals-impl0;funWithValueParameter-impl;funWithValueReturnType-impl;hashCode-impl;toString-impl]
