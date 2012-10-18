fun test() {
   <info descr="'MyClass.<class-object-for-MyClass>' is deprecated">MyClass</info>.test
   MyClass()
   val a: MyClass? = null
   val b: MyTrait? = null
   <info descr="'MyTrait.<class-object-for-MyTrait>' is deprecated">MyTrait</info>.test
}

class MyClass(): MyTrait {
    deprecated("'MyClass.<class-object-for-MyClass>' is deprecated") class object {
        val <info>test</info>: String = ""
    }
}

trait MyTrait {
    deprecated("'MyTrait.<class-object-for-MyTrait>' is deprecated") class object {
        val <info>test</info>: String = ""
    }
}