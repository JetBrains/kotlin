import kotlin.reflect.KClass

class A
class B

val a1 : KClass<*> = A::class
val a2 : KClass<A> = A::class
val a3 : KClass<B> = <!TYPE_MISMATCH!>A::class<!>
val a4 : B = <!TYPE_MISMATCH!>A::class<!>
