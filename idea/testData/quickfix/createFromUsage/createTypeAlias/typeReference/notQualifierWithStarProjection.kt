// "Create type alias 'A'" "true"
// ERROR: Unresolved reference: Dummy
package p

fun foo(): <caret>A<*, String> = throw Throwable("")