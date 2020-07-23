import kotlin.reflect.KClass

open class A
class B : A()

annotation class Ann1(val arg: KClass<A>)

@Ann1(A::class)
class MyClass1

<!INAPPLICABLE_CANDIDATE!>@Ann1(Any::class)<!>
class MyClass1a

<!INAPPLICABLE_CANDIDATE!>@Ann1(B::class)<!>
class MyClass2

annotation class Ann2(val arg: KClass<B>)

<!INAPPLICABLE_CANDIDATE!>@Ann2(A::class)<!>
class MyClass3

@Ann2(B::class)
class MyClass4
