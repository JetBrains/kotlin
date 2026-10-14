// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

@JvmInline
value class InlineClass(val value: String) {
    fun function(other: InlineClass) {}
}

value class SingleFieldValueClass(val value: String) {
    fun function(other: SingleFieldValueClass) {}
}

class Regular {
    fun inlineParameter(i: InlineClass) {}
    fun singleFieldParameter(s: SingleFieldValueClass) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: InlineClass.class[function], Regular.class[inlineParameter]
// LIGHT_ELEMENTS_NO_DECLARATION: InlineClass.class[constructor-impl;equals-impl;equals-impl0;function-_buEuXY;hashCode-impl;toString-impl], Regular.class[inlineParameter-_buEuXY]
