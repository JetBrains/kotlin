import kotlin.reflect.KClass

open class A
class B1 : A()
class B2 : A()

annotation class Ann1(val arg: KClass<out A>)

@Ann1(A::class)
class MyClass1

<!INAPPLICABLE_CANDIDATE!>@Ann1(Any::class)<!>
class MyClass1a

@Ann1(B1::class)
class MyClass2

annotation class Ann2(val arg: KClass<out B1>)

<!INAPPLICABLE_CANDIDATE!>@Ann2(A::class)<!>
class MyClass3

@Ann2(B1::class)
class MyClass4

<!INAPPLICABLE_CANDIDATE!>@Ann2(B2::class)<!>
class MyClass5
