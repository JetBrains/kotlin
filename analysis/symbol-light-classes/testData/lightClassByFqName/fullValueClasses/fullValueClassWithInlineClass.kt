// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

@JvmInline
value class InlineClass(val value: Int)

value class ValueClass(val inline: InlineClass, val regular: Int) {
    fun funWithInlineParameter(i: InlineClass) {}
    fun funWithInlineReturnType(): InlineClass = inline
    fun funWithSelfParameter(v: ValueClass) {}
    val propertyWithInlineType: InlineClass get() = inline
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: ValueClass.class[funWithInlineParameter;funWithInlineReturnType;propertyWithInlineType]
// LIGHT_ELEMENTS_NO_DECLARATION: ValueClass.class[funWithInlineParameter-_buEuXY;funWithInlineReturnType-Ww3kNBE;getInline-Ww3kNBE;getPropertyWithInlineType-Ww3kNBE]
