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
    PublicClass().<!INVISIBLE_MEMBER!>internalMemberFun<!>()
    PublicClass.<!INVISIBLE_MEMBER!>Companion<!>

    <!INVISIBLE_MEMBER!>internalFun<!>(<!INVISIBLE_MEMBER!>internalVal<!>)

    return i
}
