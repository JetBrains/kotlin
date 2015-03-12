trait MyTrait {
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

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>protected val protectedProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val internalProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val internal2Property<!> = object : MyClass(), MyTrait {}

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public val publicProperty<!> = object : MyClass(), MyTrait {}


    private fun privateFunction() = object : MyClass(), MyTrait {}

    init {
      privateFunction().f1()
      privateFunction().f2()
    }

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>protected fun protectedFunction()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun internalFunction()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun internal2Function()<!> = object : MyClass(), MyTrait {}

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public fun publicFunction()<!> = object : MyClass(), MyTrait {}



  class FooInner {
    private val privatePropertyInner = object : MyClass(), MyTrait {}

    init {
        privatePropertyInner.f1()
        privatePropertyInner.f2()
    }

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>protected val protectedProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val internalProperty<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val internal2Property<!> = object : MyClass(), MyTrait {}

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public val publicProperty<!> = object : MyClass(), MyTrait {}


    private fun privateFunctionInner() = object : MyClass(), MyTrait {}

    init {
      privateFunctionInner().f1()
      privateFunctionInner().f2()
    }

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>protected fun protectedFunction()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun internalFunction()<!> = object : MyClass(), MyTrait {}

    <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun internal2Function()<!> = object : MyClass(), MyTrait {}

    <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public fun publicFunction()<!> = object : MyClass(), MyTrait {}

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

<!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!><!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> val packageProtectedProperty<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>val packageInternalProperty<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal val packageInternal2Property<!> = object : MyClass(), MyTrait {}

<!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public val packagePublicProperty<!> = object : MyClass(), MyTrait {}

<!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!><!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> fun packageProtectedFunction()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>fun packageInternalFunction()<!> = object : MyClass(), MyTrait {}

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>internal fun packageInternal2Function()<!> = object : MyClass(), MyTrait {}

<!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public fun packagePublicFunction()<!> = object : MyClass(), MyTrait {}

fun fooPackage() {
    val packageLocalVar = object : MyClass(), MyTrait {}
    packageLocalVar.f1()
    packageLocalVar.f2()

    fun fooPackageLocal() = object : MyClass(), MyTrait {}
    fooPackageLocal().f1()
    fooPackageLocal().f2()
}