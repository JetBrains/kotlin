fun test() {
   <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyClass' is deprecated. Use A instead">MyClass</warning>.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyClass' is deprecated. Use A instead">test</warning>
   MyClass()
   val a: MyClass? = null
   val b: MyTrait? = null
   <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyTrait' is deprecated. Use A instead">MyTrait</warning>.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyTrait' is deprecated. Use A instead">test</warning>
   MyTrait.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyTrait' is deprecated. Use A instead">Companion</warning>
   <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyTrait' is deprecated. Use A instead">MyTrait</warning>
   MyClass.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyClass' is deprecated. Use A instead">Companion</warning>
   MyClass.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyClass' is deprecated. Use A instead">Companion</warning>.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'companion object of MyClass' is deprecated. Use A instead">test</warning>

   a == b
}

class MyClass(): MyTrait {
    deprecated("Use A instead") companion object {
        val test: String = ""
    }
}

interface MyTrait {
    deprecated("Use A instead") companion object {
        val test: String = ""
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS