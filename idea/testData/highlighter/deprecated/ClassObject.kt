fun test() {
   <warning descr="'MyClass.Companion' is deprecated. Use A instead">MyClass</warning>.test
   MyClass()
   val a: MyClass? = null
   val b: MyTrait? = null
   <warning descr="'MyTrait.Companion' is deprecated. Use A instead">MyTrait</warning>.test
   MyTrait.<warning descr="'MyTrait.Companion' is deprecated. Use A instead">Companion</warning>
   <warning descr="'MyTrait.Companion' is deprecated. Use A instead">MyTrait</warning>
   MyClass.<warning descr="'MyClass.Companion' is deprecated. Use A instead">Companion</warning>
   MyClass.<warning descr="'MyClass.Companion' is deprecated. Use A instead">Companion</warning>.test

   a == b
}

class MyClass(): MyTrait {
    deprecated("Use A instead") companion object {
        val test: String = ""
    }
}

trait MyTrait {
    deprecated("Use A instead") companion object {
        val test: String = ""
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS