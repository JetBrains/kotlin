open class Foo(val a: Int)

// No partial body analysis (primary constructors have no body)
class Bar : Foo(<expr>5</expr>)