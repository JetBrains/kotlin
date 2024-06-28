package b

import a.*

@Anno(E.ENTRY)
@Anno2(arrayOf(E.ENTRY))
open class B {
    @Anno(E.ENTRY)
    @Anno2(arrayOf(E.ENTRY))
    fun <@Anno(E.ENTRY) @Anno2(arrayOf(E.ENTRY)) T> foo(t: T) = t
}
