fun test() {
    val c = MyClass()
    c.<info descr="'getter for test1' is deprecated">test1</info>
    c.<info descr="'getter for test2' is deprecated">test2</info>
    c.test2 = ""

    c.<info descr="'val test3' is deprecated">test3</info>
}

class MyClass() {
    <info>public</info> val <info>test1</info>: String = ""
      [Deprecated] <info descr="null">get</info>

    <info>public</info> var <info>test2</info>: String = ""
      [Deprecated] <info descr="null">get</info>

    Deprecated <info>public</info> val <info>test3</info>: String = ""
      [Deprecated] <info>get</info>
}