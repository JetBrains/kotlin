// WITH_STDLIB

@JvmInline
value class InlineClassTest(val a: Boolean) {
    fun foo() {
        val arg0 = 42
    }
}

// METHOD : InlineClassTest.foo-impl(Z)V
// VARIABLE : NAME=$this-a TYPE=Z INDEX=0
// VARIABLE : NAME=arg0 TYPE=I INDEX=1
