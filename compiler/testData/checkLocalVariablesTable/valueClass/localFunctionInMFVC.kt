// LANGUAGE: +ValueClasses
// WITH_STDLIB

@JvmInline
value class MFVC(val a: Boolean, val b:Int) {
    fun foo() {
        fun bar() {
            a
            b
        }
    }
}

// METHOD : MFVC.foo_impl$bar(ZI)V
// VARIABLE : NAME=$$dispatchReceiver-a TYPE=Z INDEX=0
// VARIABLE : NAME=$$dispatchReceiver-b TYPE=I INDEX=1
