fun test() {
    <info descr="'val test1' is deprecated. Use A instead">test1</info>
    MyClass().<info descr="'val test2' is deprecated. Use A instead">test2</info>
    MyClass.<info descr="'val test3' is deprecated. Use A instead">test3</info>

    <info descr="'var test4' is deprecated. Use A instead">test4</info>
    MyClass().<info descr="'var test5' is deprecated. Use A instead">test5</info>
    MyClass.<info descr="'var test6' is deprecated. Use A instead">test6</info>
}

deprecated("Use A instead") val <info>test1</info>: String = ""
deprecated("Use A instead") var <info>test4</info>: String = ""

class MyClass() {
    deprecated("Use A instead") val <info>test2</info>: String = ""
    deprecated("Use A instead") var <info>test5</info>: String = ""

    class object {
         deprecated("Use A instead") val <info>test3</info>: String = ""
         deprecated("Use A instead") var <info>test6</info>: String = ""
    }
}