fun test() {
    MyClass().test1
    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning> = 0

    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning>++
    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning>--

    ++MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning>
    --MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning>

    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning> += 1
    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning> -= 1
    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning> /= 1
    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test1: Int' is deprecated. Use A instead">test1</warning> *= 1

    test2 + 1
    <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'setter for test2: Int' is deprecated. Use A instead">test2</warning> = 10
}

class MyClass() {
    public var test1: Int = 0
      @deprecated("Use A instead") set
}

public var test2: Int = 0
      @deprecated("Use A instead") set

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS