// IGNORE_FE10

// MODULE: common
// FILE: common.kt
package test

expect class Base<T>

class Foo : Base<String>

// MODULE: main()()(common)

// CLASS_ID: test/Base
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// FILE: main.kt
package test

actual class Base<T>

val f<caret>oo: Foo = Foo()
