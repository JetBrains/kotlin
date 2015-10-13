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

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>protectedProperty<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>internalProperty<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>internal2Property<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>publicProperty<!><!> = object : MyClass(), MyTrait {}


    private fun privateFunction() = object : MyClass(), MyTrait {}

    init {
      privateFunction().f1()
      privateFunction().f2()
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>protectedFunction<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>internalFunction<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>internal2Function<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>publicFunction<!>()<!> = object : MyClass(), MyTrait {}



  class FooInner {
    private val privatePropertyInner = object : MyClass(), MyTrait {}

    init {
        privatePropertyInner.f1()
        privatePropertyInner.f2()
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>protectedProperty<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>internalProperty<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>internal2Property<!><!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>publicProperty<!><!> = object : MyClass(), MyTrait {}


    private fun privateFunctionInner() = object : MyClass(), MyTrait {}

    init {
      privateFunctionInner().f1()
      privateFunctionInner().f2()
    }

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>protected fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>protectedFunction<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>internalFunction<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>internal2Function<!>()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>publicFunction<!>()<!> = object : MyClass(), MyTrait {}

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

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>private val packagePrivateProperty<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!><!WRONG_MODIFIER_TARGET!>protected<!> val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>packageProtectedProperty<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>packageInternalProperty<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>packageInternal2Property<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public val <!PROPERTY_TYPE_DEPENDS_ON_LOCAL_CLASS!>packagePublicProperty<!><!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!><!WRONG_MODIFIER_TARGET!>protected<!> fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>packageProtectedFunction<!>()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>packageInternalFunction<!>()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>packageInternal2Function<!>()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>public fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>packagePublicFunction<!>()<!> = object : MyClass(), MyTrait {}

fun fooPackage() {
    val packageLocalVar = object : MyClass(), MyTrait {}
    packageLocalVar.f1()
    packageLocalVar.f2()

    fun fooPackageLocal() = object : MyClass(), MyTrait {}
    fooPackageLocal().f1()
    fooPackageLocal().f2()
}