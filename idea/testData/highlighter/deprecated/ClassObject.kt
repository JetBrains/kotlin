fun test() {
   <info descr="'MyClass.<class-object-for-MyClass>' is deprecated. Use A instead">MyClass</info>.test
   MyClass()
   val a: MyClass? = null
   val b: MyTrait? = null
   <info descr="'MyTrait.<class-object-for-MyTrait>' is deprecated. Use A instead">MyTrait</info>.test
}

class MyClass(): MyTrait {
    deprecated("Use A instead") class object {
        val <info>test</info>: String = ""
    }
}

trait MyTrait {
    deprecated("Use A instead") class object {
        val <info>test</info>: String = ""
    }
}