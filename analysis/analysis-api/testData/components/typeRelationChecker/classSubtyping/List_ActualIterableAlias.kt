// MODULE: common
// FILE: common.kt
package test

expect class Base<T>

// MODULE: main()()(common)

// CLASS_ID: test/Base
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// FILE: main.kt
package test

actual typealias Base<T> = Iterable<T>

val l<caret>ist: List<String> = emptyList()
