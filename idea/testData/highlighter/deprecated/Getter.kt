fun test() {
    val c = MyClass()
    c.<warning descr="'getter for test1' is deprecated. Use A instead">test1</warning>
    c.<warning descr="'getter for test2' is deprecated. Use A instead">test2</warning>
    c.test2 = ""

    c.<warning descr="'val test3' is deprecated. Use A instead">test3</warning>
}

class MyClass() {
    public val test1: String = ""
      [deprecated("Use A instead")] get

    public var test2: String = ""
      [deprecated("Use A instead")] get

    deprecated("Use A instead") public val test3: String = ""
      [deprecated("Use A instead")] get
}