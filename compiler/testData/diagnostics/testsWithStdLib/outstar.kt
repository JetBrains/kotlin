// FIR_IDENTICAL
// Reproduces exception in TypeResolver.kt: EA-66870

import java.util.ArrayList

abstract class J {
    public abstract fun <T : Collection<S>, S : List<<!WRONG_MODIFIER_TARGET!>out<!> *>> foo(x: T)
    fun bar() {
        val s = ArrayList<ArrayList<Int>>()
        foo(s)
    }
}