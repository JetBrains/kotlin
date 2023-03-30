interface Lazy<T> {
    operator fun getValue(a1: Any, a2: Any): T
}

fun <T> lazy(f: () -> T): Lazy<T> = throw Exception()

interface MyTrait {
    fun f1() {}
}

open class MyClass {
    fun f2() {}
}


class Foo(val myTrait: MyTrait) {

    private val privateProperty = object : MyClass(), MyTrait {}
    val publicPropertyWithSingleSuperType = object : MyClass() {
        fun onlyFromAnonymousObject() {}
    }
    private val privatePropertyWithSingleSuperType = object : MyClass() {
        fun onlyFromAnonymousObject() {}
    }

    init {
        privateProperty.f1()
        privateProperty.f2()
        publicPropertyWithSingleSuperType.<!UNRESOLVED_REFERENCE!>onlyFromAnonymousObject<!>() // unresolved due to approximation
        privatePropertyWithSingleSuperType.onlyFromAnonymousObject() // resolvable since private
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val protectedProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val internalProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val internal2Property<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val publicProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val propertyWithGetter<!>
    get() = object: MyClass(), MyTrait {}

    private val privateDelegateProperty by lazy { object : MyClass(), MyTrait {} }
    val publicDelegatePropertyWithSingleSuperType by lazy {
        object : MyClass() {
            fun onlyFromAnonymousObject() {}
        }
    }
    private val privateDelegatePropertyWithSingleSuperType by lazy {
        object : MyClass() {
            fun onlyFromAnonymousObject() {}
        }
    }

    init {
        privateDelegateProperty.f1()
        privateDelegateProperty.f2()
        publicDelegatePropertyWithSingleSuperType.<!UNRESOLVED_REFERENCE!>onlyFromAnonymousObject<!>() // unresolved due to approximation
        privateDelegatePropertyWithSingleSuperType.onlyFromAnonymousObject() // resolvable since private
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val protectedDelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val internalDelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val internal2DelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val publicDelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    private val privateDelegate = object : MyTrait by myTrait {
        fun f2() {}
    }
    val delegate = object : MyTrait by myTrait {
        fun f2() {}
    }

    init {
        privateDelegate.f1()
        privateDelegate.f2()
        delegate.f1()
        delegate.<!UNRESOLVED_REFERENCE!>f2<!>()
    }

    private fun privateFunction() = object : MyClass(), MyTrait {}

    init {
        privateFunction().f1()
        privateFunction().f2()
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected fun protectedFunction()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun internalFunction()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun internal2Function()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun publicFunction()<!> = object : MyClass(), MyTrait {}



    class FooInner {
        private val privatePropertyInner = object : MyClass(), MyTrait {}

        init {
            privatePropertyInner.f1()
            privatePropertyInner.f2()
        }

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val protectedProperty<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val internalProperty<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val internal2Property<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val publicProperty<!> = object : MyClass(), MyTrait {}


        private fun privateFunctionInner() = object : MyClass(), MyTrait {}

        init {
            privateFunctionInner().f1()
            privateFunctionInner().f2()
        }

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected fun protectedFunction()<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun internalFunction()<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun internal2Function()<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun publicFunction()<!> = object : MyClass(), MyTrait {}

    }

    fun foo() {
        val localVar = object : MyClass(), MyTrait {}
        localVar.f1()
        localVar.f2()

        fun foo2() = object : MyClass(), MyTrait {}
        foo2().f1()
        foo2().f2()
    }

}

private val packagePrivateProperty = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!><!WRONG_MODIFIER_TARGET!>protected<!> val packageProtectedProperty<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val packageInternalProperty<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val packageInternal2Property<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val packagePublicProperty<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!><!WRONG_MODIFIER_TARGET!>protected<!> fun packageProtectedFunction()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun packageInternalFunction()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun packageInternal2Function()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun packagePublicFunction()<!> = object : MyClass(), MyTrait {}

fun fooPackage() {
    val packageLocalVar = object : MyClass(), MyTrait {}
    packageLocalVar.f1()
    packageLocalVar.f2()

    fun fooPackageLocal() = object : MyClass(), MyTrait {}
    fooPackageLocal().f1()
    fooPackageLocal().f2()
}
