// IGNORE_REVERSED_RESOLVE
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

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val <!EXPOSED_PROPERTY_TYPE!>protectedProperty<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!EXPOSED_PROPERTY_TYPE!>internalProperty<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!EXPOSED_PROPERTY_TYPE!>internal2Property<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!EXPOSED_PROPERTY_TYPE!>publicProperty<!><!> = object : MyClass(), MyTrait {}

    val <!EXPOSED_PROPERTY_TYPE!>propertyWithGetter<!>
    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>get()<!> = object: MyClass(), MyTrait {}

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
        <!DEBUG_INFO_LEAKING_THIS!>privateDelegateProperty<!>.f1()
        <!DEBUG_INFO_LEAKING_THIS!>privateDelegateProperty<!>.f2()
        <!DEBUG_INFO_LEAKING_THIS!>publicDelegatePropertyWithSingleSuperType<!>.<!UNRESOLVED_REFERENCE!>onlyFromAnonymousObject<!>() // unresolved due to approximation
        <!DEBUG_INFO_LEAKING_THIS!>privateDelegatePropertyWithSingleSuperType<!>.onlyFromAnonymousObject() // resolvable since private
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val <!EXPOSED_PROPERTY_TYPE!>protectedDelegateProperty<!><!> by lazy { object : MyClass(), MyTrait {} }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!EXPOSED_PROPERTY_TYPE!>internalDelegateProperty<!><!> by lazy { object : MyClass(), MyTrait {} }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!EXPOSED_PROPERTY_TYPE!>internal2DelegateProperty<!><!> by lazy { object : MyClass(), MyTrait {} }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!EXPOSED_PROPERTY_TYPE!>publicDelegateProperty<!><!> by lazy { object : MyClass(), MyTrait {} }

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

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>protectedFunction<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun <!EXPOSED_FUNCTION_RETURN_TYPE!>internalFunction<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>internal2Function<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun <!EXPOSED_FUNCTION_RETURN_TYPE!>publicFunction<!>()<!> = object : MyClass(), MyTrait {}



    class FooInner {
        private val privatePropertyInner = object : MyClass(), MyTrait {}

        init {
            privatePropertyInner.f1()
            privatePropertyInner.f2()
        }

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val <!EXPOSED_PROPERTY_TYPE!>protectedProperty<!><!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!EXPOSED_PROPERTY_TYPE!>internalProperty<!><!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!EXPOSED_PROPERTY_TYPE!>internal2Property<!><!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!EXPOSED_PROPERTY_TYPE!>publicProperty<!><!> = object : MyClass(), MyTrait {}


        private fun privateFunctionInner() = object : MyClass(), MyTrait {}

        init {
            privateFunctionInner().f1()
            privateFunctionInner().f2()
        }

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>protectedFunction<!>()<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun <!EXPOSED_FUNCTION_RETURN_TYPE!>internalFunction<!>()<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>internal2Function<!>()<!> = object : MyClass(), MyTrait {}

        <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun <!EXPOSED_FUNCTION_RETURN_TYPE!>publicFunction<!>()<!> = object : MyClass(), MyTrait {}

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

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!><!WRONG_MODIFIER_TARGET!>protected<!> val <!EXPOSED_PROPERTY_TYPE!>packageProtectedProperty<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!EXPOSED_PROPERTY_TYPE!>packageInternalProperty<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!EXPOSED_PROPERTY_TYPE!>packageInternal2Property<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!EXPOSED_PROPERTY_TYPE!>packagePublicProperty<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!><!WRONG_MODIFIER_TARGET!>protected<!> fun <!EXPOSED_FUNCTION_RETURN_TYPE!>packageProtectedFunction<!>()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun <!EXPOSED_FUNCTION_RETURN_TYPE!>packageInternalFunction<!>()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>packageInternal2Function<!>()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun <!EXPOSED_FUNCTION_RETURN_TYPE!>packagePublicFunction<!>()<!> = object : MyClass(), MyTrait {}

fun fooPackage() {
    val packageLocalVar = object : MyClass(), MyTrait {}
    packageLocalVar.f1()
    packageLocalVar.f2()

    fun fooPackageLocal() = object : MyClass(), MyTrait {}
    fooPackageLocal().f1()
    fooPackageLocal().f2()
}
