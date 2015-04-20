fun test() {
    val c = MyClass()
    c.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'getter for test1: String' is deprecated. Use A instead">test1</warning>
    c.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'getter for test2: String' is deprecated. Use A instead">test2</warning>
    c.test2 = ""

    c.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'test3: String' is deprecated. Use A instead">test3</warning>
}

class MyClass() {
    public val test1: String = ""
      [deprecated("Use A instead")] get

    public var test2: String = ""
      [deprecated("Use A instead")] get

    deprecated("Use A instead") public val test3: String = ""
      [deprecated("Use A instead")] get
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS