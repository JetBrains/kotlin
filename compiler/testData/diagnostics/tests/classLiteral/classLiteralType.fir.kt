import kotlin.reflect.KClass

class A
class B

val listOfString: List<String> = null!!
val arrayOfString: Array<String> = null!!

val a1 : KClass<*> = A::class
val a2 : KClass<A> = A::class
val a3 : KClass<B> = A::class
val a4 : B = A::class

val a5 : KClass<out List<String>> = listOfString::class
val a6 : KClass<out Array<String>> = arrayOfString::class
