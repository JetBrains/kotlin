// FILE: annotation.kt
package kotlin.native

annotation class Throws(vararg val exceptionClasses: kotlin.reflect.KClass<out Throwable>)

// FILE: test.kt
class Exception1 : Throwable()
class Exception2 : Throwable()
class Exception3 : Throwable()

<!THROWS_LIST_EMPTY!>@Throws<!>
fun foo() {}

interface Base1 {
    @Throws(Exception1::class) fun foo()
}

class HasThrowsOnOverride : Base1 {
    <!THROWS_ON_OVERRIDE!>@Throws(Exception1::class)<!> override fun foo() {}
}

class HasThrowsWithEmptyListOnOverride : Base1 {
    <!THROWS_ON_OVERRIDE!>@Throws<!> override fun foo() {}
}

interface Base2 {
    @Throws(Exception2::class) fun foo()
}

open class InheritsDifferentThrows1 : Base1, Base2 {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

class InheritsDifferentThrowsThroughSameClass1 : InheritsDifferentThrows1() {
    override fun foo() {}
}

interface Base3 {
    @Throws(Exception3::class) fun foo()
}

class InheritsDifferentThrows2 : InheritsDifferentThrows1(), Base3 {
    <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
}

interface Base4 {
    @Throws(Exception1::class) fun foo()
}

class InheritsSameThrows : Base1, Base4 {
    override fun foo() {}
}

interface Base5 {
    @Throws(Exception1::class, Exception2::class) fun foo()
}

interface Base6 {
    @Throws(Exception2::class, Exception1::class) fun foo()
}

class InheritsSameThrowsMultiple : Base5, Base6 {
    override fun foo() {}
}

fun withLocalClass() {
    class LocalException : Throwable()

    abstract class Base7 {
        @Throws(Exception1::class, LocalException::class) abstract fun foo()
    }

    class InheritsDifferentThrowsLocal : Base1, Base7() {
        <!INCOMPATIBLE_THROWS_INHERITED!>override fun foo() {}<!>
    }
}
