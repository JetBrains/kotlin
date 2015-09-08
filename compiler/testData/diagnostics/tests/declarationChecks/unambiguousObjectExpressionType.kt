open class MyClass {
  fun f1() {}
}


class Foo {

    protected val protectedProperty = object : MyClass() {}

    public val publicProperty = object : MyClass() {}

    protected val protected2Property : MyClass = object : MyClass() {fun invisible() {}}

    public val public2Property : MyClass = object : MyClass() {fun invisible() {}}

    private val privateProperty = object : MyClass() {fun visible() {}}

    internal val internalProperty = object : MyClass() { fun invisible() {}}


    fun testProperties() {
      privateProperty.f1()
      internalProperty.f1()
      protected2Property.f1()
      public2Property.f1()

      privateProperty.visible()
      protected2Property.<!UNRESOLVED_REFERENCE!>invisible<!>()
      public2Property.<!UNRESOLVED_REFERENCE!>invisible<!>()
      internalProperty.<!UNRESOLVED_REFERENCE!>invisible<!>()
    }


    protected fun protectedFunction() = object : MyClass() {}

    public fun publicFunction() = object : MyClass() {}

    protected fun protected2Function(): MyClass = object : MyClass() {fun visible() {}}

    public fun public2Function(): MyClass = object : MyClass() {fun visible() {}}

    private fun privateFunction() = object : MyClass() {fun visible() {}}

    internal fun internalFunction() = object : MyClass() {fun invisible() {}}


    fun testFunctions() {
      privateFunction().f1()
      internalFunction().f1()
      public2Function().f1()
      protected2Function().f1()

      privateFunction().visible()
      internalFunction().<!UNRESOLVED_REFERENCE!>invisible<!>()
      public2Function().<!UNRESOLVED_REFERENCE!>invisible<!>()
      protected2Function().<!UNRESOLVED_REFERENCE!>invisible<!>()
    }


    class FooInner {

        public val publicProperty = object : MyClass() {}

        protected val protectedProperty = object : MyClass() {}

        protected val protected2Property : MyClass = object : MyClass() {fun invisible() {}}

        public val public2Property : MyClass = object : MyClass() {fun invisible() {}}

        private val privateProperty = object : MyClass() {fun visible() {}}

        internal val internalProperty = object : MyClass() { fun invisible() {}}


        fun testProperties() {
          privateProperty.f1()
          internalProperty.f1()
          protected2Property.f1()
          public2Property.f1()

          privateProperty.visible()
          protected2Property.<!UNRESOLVED_REFERENCE!>invisible<!>()
          public2Property.<!UNRESOLVED_REFERENCE!>invisible<!>()
          internalProperty.<!UNRESOLVED_REFERENCE!>invisible<!>()
        }


         protected fun protectedFunction() = object : MyClass() {}

         public fun publicFunction() = object : MyClass() {}

         protected fun protected2Function(): MyClass = object : MyClass() {fun visible() {}}

         public fun public2Function(): MyClass = object : MyClass() {fun visible() {}}

         private fun privateFunction() = object : MyClass() {fun visible() {}}

         internal fun internalFunction() = object : MyClass() {fun invisible() {}}


         fun testFunctions() {
           privateFunction().f1()
           internalFunction().f1()
           public2Function().f1()
           protected2Function().f1()

           privateFunction().visible()
           internalFunction().<!UNRESOLVED_REFERENCE!>invisible<!>()
           public2Function().<!UNRESOLVED_REFERENCE!>invisible<!>()
           protected2Function().<!UNRESOLVED_REFERENCE!>invisible<!>()
         }
    }

    fun foo() {
      val localVar = object : MyClass() {}
      localVar.f1()
      fun foo2() = object : MyClass() {}
      foo2().f1()
    }

}

<!WRONG_MODIFIER_TARGET!>protected<!> val packageProtectedProperty = object : MyClass() {}

public val packagePublicProperty = object : MyClass() {}

public val packagePublic2Property : MyClass = object : MyClass() {fun invisible() {}}

private val packagePrivateProperty = object : MyClass() {fun invisible() {}}

internal val packageInternalProperty = object : MyClass() {fun invisible() {}}


fun testProperties() {
    packagePrivateProperty.f1()
    packageInternalProperty.f1()
    packagePublic2Property.f1()

    packagePrivateProperty.<!UNRESOLVED_REFERENCE!>invisible<!>()
    packageInternalProperty.<!UNRESOLVED_REFERENCE!>invisible<!>()
    packagePublic2Property.<!UNRESOLVED_REFERENCE!>invisible<!>()
}


private fun privateFunction() = object : MyClass() {fun invisible() {}}

<!WRONG_MODIFIER_TARGET!>protected<!> fun protectedFunction() = object : MyClass() {}

public fun publicFunction() = object : MyClass() {}

public fun public2Function() : MyClass = object : MyClass() {fun invisible() {}}

internal fun internalFunction() = object : MyClass() {fun invisible() {}}



fun testFunctions() {
  privateFunction().f1()
  internalFunction().f1()
  public2Function().f1()

  privateFunction().<!UNRESOLVED_REFERENCE!>invisible<!>()
  internalFunction().<!UNRESOLVED_REFERENCE!>invisible<!>()
  public2Function().<!UNRESOLVED_REFERENCE!>invisible<!>()
}

fun fooPackage() {
    val packageLocalVar = object : MyClass() {fun visible() {}}
    packageLocalVar.f1()
    packageLocalVar.visible()

    fun fooPackageLocal() = object : MyClass() {fun visible() {}}
    fooPackageLocal().f1()
    fooPackageLocal().visible()
}