interface MyTrait {
    fun f1() {}
}

open class MyClass {
    fun f2() {}
}


class Foo {

    private val privateProperty = object : MyClass(), MyTrait {}

    init {
        privateProperty.f1()
        privateProperty.f2()
    }

    protected val protectedProperty = object : MyClass(), MyTrait {}

    val internalProperty = object : MyClass(), MyTrait {}

    internal val internal2Property = object : MyClass(), MyTrait {}

    public val publicProperty = object : MyClass(), MyTrait {}

    val propertyWithGetter
    get() = object: MyClass(), MyTrait {}


    private fun privateFunction() = object : MyClass(), MyTrait {}

    init {
        privateFunction().f1()
        privateFunction().f2()
    }

    protected fun protectedFunction() = object : MyClass(), MyTrait {}

    fun internalFunction() = object : MyClass(), MyTrait {}

    internal fun internal2Function() = object : MyClass(), MyTrait {}

    public fun publicFunction() = object : MyClass(), MyTrait {}



    class FooInner {
        private val privatePropertyInner = object : MyClass(), MyTrait {}

        init {
            privatePropertyInner.f1()
            privatePropertyInner.f2()
        }

        protected val protectedProperty = object : MyClass(), MyTrait {}

        val internalProperty = object : MyClass(), MyTrait {}

        internal val internal2Property = object : MyClass(), MyTrait {}

        public val publicProperty = object : MyClass(), MyTrait {}


        private fun privateFunctionInner() = object : MyClass(), MyTrait {}

        init {
            privateFunctionInner().f1()
            privateFunctionInner().f2()
        }

        protected fun protectedFunction() = object : MyClass(), MyTrait {}

        fun internalFunction() = object : MyClass(), MyTrait {}

        internal fun internal2Function() = object : MyClass(), MyTrait {}

        public fun publicFunction() = object : MyClass(), MyTrait {}

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

<!WRONG_MODIFIER_TARGET!>protected<!> val packageProtectedProperty = object : MyClass(), MyTrait {}

val packageInternalProperty = object : MyClass(), MyTrait {}

internal val packageInternal2Property = object : MyClass(), MyTrait {}

public val packagePublicProperty = object : MyClass(), MyTrait {}

<!WRONG_MODIFIER_TARGET!>protected<!> fun packageProtectedFunction() = object : MyClass(), MyTrait {}

fun packageInternalFunction() = object : MyClass(), MyTrait {}

internal fun packageInternal2Function() = object : MyClass(), MyTrait {}

public fun packagePublicFunction() = object : MyClass(), MyTrait {}

fun fooPackage() {
    val packageLocalVar = object : MyClass(), MyTrait {}
    packageLocalVar.f1()
    packageLocalVar.f2()

    fun fooPackageLocal() = object : MyClass(), MyTrait {}
    fooPackageLocal().f1()
    fooPackageLocal().f2()
}
