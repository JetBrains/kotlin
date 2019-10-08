interface SomeInterface {
    fun foo(x: Int, y: String): String

//      Boolean
//      │
    val bar: Boolean
}

class SomeClass : SomeInterface {
//              Int   Int
//              │     │
    private val baz = 42

    override fun foo(x: Int, y: String): String {
//             SomeClass.foo.y: String
//             │ fun (String).plus(Any?): String
//             │ │ SomeClass.foo.x: Int
//             │ │ │ fun (String).plus(Any?): String
//             │ │ │ │ val (SomeClass).baz: Int
//             │ │ │ │ │
        return y + x + baz
    }

//               Boolean
//               │
    override var bar: Boolean
//              Boolean
//              │
        get() = true
//          Boolean
//          │
        set(value) {}

//               Double
//               │
    lateinit var fau: Double
}

inline class InlineClass
