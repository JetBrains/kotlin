deprecated("Use A instead") trait MyTrait { }

fun test() {
   val a: <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyTrait' is deprecated. Use A instead">MyTrait</warning>? = null
   val b: List<<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyTrait' is deprecated. Use A instead">MyTrait</warning>>? = null
   a == b
}

class Test(): <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyTrait' is deprecated. Use A instead">MyTrait</warning> { }

class Test2(<warning descr="[UNUSED_PARAMETER] Parameter 'param' is never used">param</warning>: <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyTrait' is deprecated. Use A instead">MyTrait</warning>) {}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS