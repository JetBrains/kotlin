// Should stop on primary constructor invocation
class Foo1(val a: Int) /// M

class Foo2( /// M
    val a: Int, /// F
    val b: String /// F
)

class Foo3(val a: Int) { /// M
    constructor(a: String) : this(a.toInt()) /// M
}

// Initializers are not currently recognized as functions
class Foo4 { /// M
    init { /// L
        println() /// L
    } /// L
}

class Foo5 {
    constructor(a: String) {} /// M
    constructor(a: Int) {} /// M
}

interface Intf

annotation class Anno

enum class Enum1 { /// M
    FOO
}

enum class Enum2(val a: Int) { /// M
    FOO(1)
}

object Obj1

object Obj2 {}