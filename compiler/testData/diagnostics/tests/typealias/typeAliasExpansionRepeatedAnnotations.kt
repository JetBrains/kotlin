// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class A

typealias AInt = @A Int
typealias AI = AInt

typealias Test1 = <!REPEATED_ANNOTATION!>@A<!> AInt
typealias Test2 = <!REPEATED_ANNOTATION!>@A<!> AI
typealias Test3 = List<<!REPEATED_ANNOTATION!>@A<!> AInt>
typealias Test4 = List<<!REPEATED_ANNOTATION!>@A<!> AI>

val testProperty1: <!REPEATED_ANNOTATION!>@A<!> AInt = 0
val testProperty2: <!REPEATED_ANNOTATION!>@A<!> AI = 0

fun testFunction1(x: <!REPEATED_ANNOTATION!>@A<!> AInt): <!REPEATED_ANNOTATION!>@A<!> AInt = x
fun testFunction2(x: <!REPEATED_ANNOTATION!>@A<!> AI): <!REPEATED_ANNOTATION!>@A<!> AI = x