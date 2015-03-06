fun test() {
    <warning descr="'val test1' is deprecated. Use A instead">test1</warning> == ""
    MyClass().<warning descr="'val test2' is deprecated. Use A instead">test2</warning>
    MyClass.<warning descr="'val test3' is deprecated. Use A instead">test3</warning>

    <warning descr="'var test4' is deprecated. Use A instead">test4</warning> == ""
    MyClass().<warning descr="'var test5' is deprecated. Use A instead">test5</warning>
    MyClass.<warning descr="'var test6' is deprecated. Use A instead">test6</warning>
}

deprecated("Use A instead") val test1: String = ""
deprecated("Use A instead") var test4: String = ""

class MyClass() {
    deprecated("Use A instead") val test2: String = ""
    deprecated("Use A instead") var test5: String = ""

    default object {
         deprecated("Use A instead") val test3: String = ""
         deprecated("Use A instead") var test6: String = ""
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS