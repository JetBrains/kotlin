// FIR_IDENTICAL
open class Base {
    companion object
}
interface IFoo
open class CG<T>
interface IG<T>

@Deprecated("Obsolete")
typealias Obsolete = Base

@Deprecated("Obsolete")
typealias IObsolete = IFoo

fun test1(x: <!DEPRECATION!>Obsolete<!>) = x
fun test1a(x: List<<!DEPRECATION!>Obsolete<!>>) = x

val test2 = <!DEPRECATION!>Obsolete<!>()

val test3 = <!DEPRECATION!>Obsolete<!>

class Test4: <!DEPRECATION!>Obsolete<!>()
class Test4a: <!DEPRECATION!>IObsolete<!>
class Test4b: IG<<!DEPRECATION!>Obsolete<!>>
class Test4c: CG<<!DEPRECATION!>Obsolete<!>>()