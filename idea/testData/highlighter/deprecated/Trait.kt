Deprecated trait MyTrait { }

fun test() {
   val a: <info descr="'MyTrait' is deprecated">MyTrait</info>? = null
   val b: List<<info descr="'MyTrait' is deprecated">MyTrait</info>>? = null
}

class Test(): <info descr="'MyTrait' is deprecated">MyTrait</info> { }

class Test2(param: <info descr="'MyTrait' is deprecated">MyTrait</info>) {}