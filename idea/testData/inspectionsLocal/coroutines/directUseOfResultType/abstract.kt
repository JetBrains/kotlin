// FIX: Unwrap 'Result' return type (breaks use-sites!)
package kotlin

abstract class Abstract {
    abstract fun <caret>foo(): Result<Int>
}
