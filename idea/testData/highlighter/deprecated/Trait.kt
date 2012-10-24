deprecated("Use A instead") trait MyTrait { }

fun test() {
   val a: <info descr="'MyTrait' is deprecated. Use A instead">MyTrait</info>? = null
   val b: List<<info descr="'MyTrait' is deprecated. Use A instead">MyTrait</info>>? = null
}

class Test(): <info descr="'MyTrait' is deprecated. Use A instead">MyTrait</info> { }

class Test2(param: <info descr="'MyTrait' is deprecated. Use A instead">MyTrait</info>) {}