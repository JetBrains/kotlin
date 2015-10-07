@Deprecated("Use A instead") interface MyTrait { }

fun test() {
   val a: <warning descr="[DEPRECATION] 'MyTrait' is deprecated. Use A instead">MyTrait</warning>? = null
   val b: List<<warning descr="[DEPRECATION] 'MyTrait' is deprecated. Use A instead">MyTrait</warning>>? = null
   a == b
}

class Test(): <warning descr="[DEPRECATION] 'MyTrait' is deprecated. Use A instead">MyTrait</warning> { }

class Test2(<warning descr="[UNUSED_PARAMETER] Parameter 'param' is never used">param</warning>: <warning descr="[DEPRECATION] 'MyTrait' is deprecated. Use A instead">MyTrait</warning>) {}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
