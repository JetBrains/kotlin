import kotlin.reflect.KClass

open class A
class B : A()

annotation class Ann1(val arg: KClass<A>)

@Ann1(A::class)
class MyClass1

@Ann1(<!TYPE_MISMATCH!>Any::class<!>)
class MyClass1a

@Ann1(<!TYPE_MISMATCH!>B::class<!>)
class MyClass2

annotation class Ann2(val arg: KClass<B>)

@Ann2(<!TYPE_MISMATCH!>A::class<!>)
class MyClass3

@Ann2(B::class)
class MyClass4
