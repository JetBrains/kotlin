@Deprecated("")
class Foo

typealias Test1 = <!DEPRECATION!>Foo<!>
typealias Test2 = List<<!DEPRECATION!>Foo<!>>
typealias Test3 = List<<!DEPRECATION!>Test2<!>>

fun use1(b: <!DEPRECATION!>Test1<!>) = b
fun use2(b: <!DEPRECATION!>Test2<!>) = b
fun use3(b: <!DEPRECATION!>Test3<!>) = b