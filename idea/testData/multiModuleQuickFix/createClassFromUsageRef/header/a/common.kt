// "Create class 'ClassG'" "true"
// ERROR: Unresolved reference: ClassG

package a

fun test() {
    a.b.ClassG<caret>()
}
