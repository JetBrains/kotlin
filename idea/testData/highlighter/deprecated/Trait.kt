deprecated("Use A instead") trait MyTrait { }

fun test() {
   val a: <warning descr="'MyTrait' is deprecated. Use A instead">MyTrait</warning>? = null
   val b: List<<warning descr="'MyTrait' is deprecated. Use A instead">MyTrait</warning>>? = null
   a == b
}

class Test(): <warning descr="'MyTrait' is deprecated. Use A instead">MyTrait</warning> { }

class Test2(param: <warning descr="'MyTrait' is deprecated. Use A instead">MyTrait</warning>) {}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS