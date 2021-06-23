@Deprecated("")
class Foo

@Deprecated("", level = DeprecationLevel.ERROR)
class Err

typealias Test1 = <!DEPRECATION!>Foo<!>
typealias Test2 = List<<!DEPRECATION!>Foo<!>>
typealias Test3 = List<Test2>

typealias TestErr1 = <!DEPRECATION_ERROR!>Err<!>
typealias TestErr2 = List<<!DEPRECATION_ERROR!>Err<!>>
typealias TestErr3 = List<TestErr2>

fun use1(b: Test1) = b
fun use2(b: Test2) = b
fun use3(b: Test3) = b

fun useErr1(b: TestErr1) = b
fun useErr2(b: TestErr2) = b
fun useErr3(b: TestErr3) = b
