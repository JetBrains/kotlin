fun test() {
    val c = MyClass()
    c.<info descr="'getter for test1' is deprecated. Use A instead">test1</info>
    c.<info descr="'getter for test2' is deprecated. Use A instead">test2</info>
    c.test2 = ""

    c.<info descr="'val test3' is deprecated. Use A instead">test3</info>
}

class MyClass() {
    <info>public</info> val <info>test1</info>: String = ""
      [deprecated("Use A instead")] <info>get</info>

    <info>public</info> var <info>test2</info>: String = ""
      [deprecated("Use A instead")] <info>get</info>

    deprecated("Use A instead") <info>public</info> val <info>test3</info>: String = ""
      [deprecated("Use A instead")] <info>get</info>
}