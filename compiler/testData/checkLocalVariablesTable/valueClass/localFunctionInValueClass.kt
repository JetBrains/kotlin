// WITH_STDLIB

@JvmInline
value class InlineClassTest(val a: Boolean) {
    fun foo() {
        fun bar() {
            a
        }
        val arg0 = 42
    }
}

// METHOD : InlineClassTest.foo_impl$bar(Z)V
// VARIABLE : NAME=$$this-a TYPE=Z INDEX=0