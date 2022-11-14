enum class EnumToClass {
    Foo,
    Bar
}

object ObjectToEnum {
    class Foo
    object Bar
}

class ClassToEnum {
    class Foo
    object Bar
    inner class Baz
}

class ClassToObject
object ObjectToClass
