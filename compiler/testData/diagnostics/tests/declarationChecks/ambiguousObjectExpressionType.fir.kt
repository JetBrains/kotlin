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

    protected val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protectedProperty<!> = object : MyClass(), MyTrait {}

    val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internalProperty<!> = object : MyClass(), MyTrait {}

    internal val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal2Property<!> = object : MyClass(), MyTrait {}

    public val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>publicProperty<!> = object : MyClass(), MyTrait {}

    val propertyWithGetter
    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>get<!>() = object: MyClass(), MyTrait {}

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

    protected val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protectedDelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internalDelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    internal val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal2DelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

    public val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>publicDelegateProperty<!> by lazy { object : MyClass(), MyTrait {} }

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

    protected fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protectedFunction<!>() = object : MyClass(), MyTrait {}

    fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internalFunction<!>() = object : MyClass(), MyTrait {}

    internal fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal2Function<!>() = object : MyClass(), MyTrait {}

    public fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>publicFunction<!>() = object : MyClass(), MyTrait {}



    class FooInner {
        private val privatePropertyInner = object : MyClass(), MyTrait {}

        init {
            privatePropertyInner.f1()
            privatePropertyInner.f2()
        }

        protected val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protectedProperty<!> = object : MyClass(), MyTrait {}

        val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internalProperty<!> = object : MyClass(), MyTrait {}

        internal val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal2Property<!> = object : MyClass(), MyTrait {}

        public val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>publicProperty<!> = object : MyClass(), MyTrait {}


        private fun privateFunctionInner() = object : MyClass(), MyTrait {}

        init {
            privateFunctionInner().f1()
            privateFunctionInner().f2()
        }

        protected fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protectedFunction<!>() = object : MyClass(), MyTrait {}

        fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internalFunction<!>() = object : MyClass(), MyTrait {}

        internal fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal2Function<!>() = object : MyClass(), MyTrait {}

        public fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>publicFunction<!>() = object : MyClass(), MyTrait {}

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

<!WRONG_MODIFIER_TARGET!>protected<!> val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packageProtectedProperty<!> = object : MyClass(), MyTrait {}

val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packageInternalProperty<!> = object : MyClass(), MyTrait {}

internal val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packageInternal2Property<!> = object : MyClass(), MyTrait {}

public val <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packagePublicProperty<!> = object : MyClass(), MyTrait {}

<!WRONG_MODIFIER_TARGET!>protected<!> fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packageProtectedFunction<!>() = object : MyClass(), MyTrait {}

fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packageInternalFunction<!>() = object : MyClass(), MyTrait {}

internal fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packageInternal2Function<!>() = object : MyClass(), MyTrait {}

public fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>packagePublicFunction<!>() = object : MyClass(), MyTrait {}

fun fooPackage() {
    val packageLocalVar = object : MyClass(), MyTrait {}
    packageLocalVar.f1()
    packageLocalVar.f2()

    fun fooPackageLocal() = object : MyClass(), MyTrait {}
    fooPackageLocal().f1()
    fooPackageLocal().f2()
}
