// "Create type alias 'A'" "true"
// ERROR: Unresolved reference: Dummy
package p

class T {

}

fun foo(): T.<caret>A = throw Throwable("")