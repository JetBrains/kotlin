// FIR_IDENTICAL
// ISSUE: KT-61095

package org.example

interface Base<P> {
    fun child(props: Int = 10)
}

interface Intermediate<P> : Base<P>

class Implementation<P>() : Intermediate<P>, Base<P> {
    override fun child(props: Int) {}
}
