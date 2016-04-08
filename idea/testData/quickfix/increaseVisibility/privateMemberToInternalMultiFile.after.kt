// "Make foo internal" "true"
// ERROR: Cannot access 'foo': it is 'private' in 'First'

package test

class Second(val f: First) {
    fun bar() = f.<caret>foo()
}
