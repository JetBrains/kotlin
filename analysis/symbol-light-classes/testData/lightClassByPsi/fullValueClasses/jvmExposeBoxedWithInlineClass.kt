// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

package pack

@JvmInline
value class InlineClass(val value: Int)

value class ValueClass(val inline: InlineClass, val regular: Int) {
    fun funWithInlineParameter(i: InlineClass) {}
    fun funWithInlineReturnType(): InlineClass = inline
    fun funWithSelfParameter(v: ValueClass) {}
}

class Regular(val value: ValueClass, val inline: InlineClass)

// LIGHT_ELEMENTS_NO_DECLARATION: InlineClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Regular.class[getInline-Ww3kNBE], ValueClass.class[funWithInlineParameter-_buEuXY;funWithInlineReturnType-Ww3kNBE;getInline-Ww3kNBE]
