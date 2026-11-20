// MODULE: lib
// LANGUAGE: +ExportKDocDocumentationToKlib
// TARGET_PLATFORM: JS
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

/**
 * Type alias KDoc.
 */
typealias Alias = String

enum class Color {
    /**
     * Enum entry KDoc.
     */
    RED,
    GREEN
}

/**
 * Property KDoc.
 */
var prop: Int
    /**
     * Getter KDoc.
     */
    get() = 0
    /**
     * Setter KDoc.
     */
    set(value) {}

var defaultAccessors: Int = 0

val bodylessAccessor: Int = 0
    /**
     * Bodyless getter KDoc.
     */
    get

var bodylessSetter: Int = 0
    /**
     * Bodyless setter KDoc.
     */
    set

// MODULE: main(lib)
// FILE: main.kt
import lib.Alias
import lib.Color
import lib.prop
import lib.defaultAccessors
import lib.bodylessAccessor
import lib.bodylessSetter

fun test(a: <expr_alias>Alias</expr_alias>) {
    <expr_entry>Color.RED</expr_entry>
    <expr_undocumentedEntry>Color.GREEN</expr_undocumentedEntry>
    <expr_prop>prop</expr_prop>
    <expr_defaultAccessors>defaultAccessors</expr_defaultAccessors>
    <expr_bodylessAccessor>bodylessAccessor</expr_bodylessAccessor>
    <expr_bodylessSetter>bodylessSetter</expr_bodylessSetter>
}
