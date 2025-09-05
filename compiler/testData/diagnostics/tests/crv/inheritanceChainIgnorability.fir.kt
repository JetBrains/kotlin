// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

interface I1 {
    fun foo(): String // Implicitly @Ignorable
    fun bar(): String // Implicitly @Ignorable
}

@MustUseReturnValue
interface I2: I1 {
    override fun foo(): String // @MustUse
}

interface I3: I2 {
    override fun foo(): String // @MustUse due to copying
    override fun bar(): String // @Ignorable due to copying
}

interface I4: I1, I2 { // Implementation detail: we always copy from first in the list
    override fun foo(): String // @Ignorable, copied from I1
    override fun bar(): String // @Ignorable due to copying
}

interface I5: I2 // result should be the same as I3 because we resolve to base functions

fun testFoo(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) {
    i1.foo()
    i2.<!RETURN_VALUE_NOT_USED!>foo<!>()
    i3.<!RETURN_VALUE_NOT_USED!>foo<!>()
    i4.foo()
    i5.<!RETURN_VALUE_NOT_USED!>foo<!>()
}

fun testBar(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5) {
    i1.bar()
    i2.bar()
    i3.bar()
    i4.bar()
    i5.bar()
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, override */
