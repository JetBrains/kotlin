annotation class AnnC(val c: Class<*>)

AnnC(<!ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL!>c<!>)
class Test

AnnC(javaClass<A>())
class Test2

val c: Class<*> = javaClass<A>()

class A
