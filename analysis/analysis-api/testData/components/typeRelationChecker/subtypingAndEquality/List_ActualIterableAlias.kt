// MODULE: common
// FILE: common.kt
package test

expect class Base<T>

// MODULE: main()()(common)

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/Base
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true

// FILE: main.kt
package test

actual typealias Base<T> = Iterable<T>

val l<caret_type1>ist: List<String> = emptyList()

val i<caret_type2>terable: Base<String> = emptyList()
