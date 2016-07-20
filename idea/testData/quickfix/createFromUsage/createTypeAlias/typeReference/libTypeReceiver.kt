// "Create type alias 'A'" "false"
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: A
package p

internal fun foo(): Int.<caret>A = throw Throwable("")