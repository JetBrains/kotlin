// FIR_IDENTICAL
// WITH_STDLIB

import kotlin.jvm.JvmDelegateToDefaults

interface Interface {
    fun foo() {}
}

class A: Interface {
    override fun foo() {}
}

class B(val a: A): Interface by @JvmDelegateToDefaults a

class C: Interface by (@JvmDelegateToDefaults object: Interface {})

val bad1 = <!WRONG_ANNOTATION_TARGET!>@JvmDelegateToDefaults<!> 1
val bad2 = <!WRONG_ANNOTATION_TARGET!>@JvmDelegateToDefaults<!> object : Interface {}
val bad3 = <!WRONG_ANNOTATION_TARGET!>@JvmDelegateToDefaults<!> A()
val bad4 = listOf(<!WRONG_ANNOTATION_TARGET!>@JvmDelegateToDefaults<!> A())[0]
