// MODULE: library
// FILE: a.kt
package a

internal interface InternalInterface

public class PublicClass {
    internal fun internalMemberFun() {}

    internal companion object {}
}

internal val internalVal = ""

internal fun internalFun(s: String): String = s

internal typealias InternalTypealias = InternalInterface

// MODULE: main(library)
// FILE: source.kt
import a.*

private fun test(i: <!INVISIBLE_REFERENCE!>InternalInterface<!>): <!INVISIBLE_REFERENCE!>InternalTypealias<!> {
    PublicClass().<!INVISIBLE_REFERENCE!>internalMemberFun<!>()
    PublicClass.<!INVISIBLE_REFERENCE!>Companion<!>

    <!INVISIBLE_REFERENCE!>internalFun<!>(<!INVISIBLE_REFERENCE!>internalVal<!>)

    return i
}
