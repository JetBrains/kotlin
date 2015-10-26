// "Create property 'foo' as constructor parameter" "true"

class A<T>(val n: T) {
    inner class B<U>(val m: U) {
        fun test(): A<Int> {
            return this.<caret>foo
        }
    }
}
