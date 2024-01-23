open class Foo<T>(val item: T)

class Bar(str: String) : <!DEBUG_INFO_CALLABLE_OWNER("Foo.Foo in Foo")!>Foo<String>(str)<!>
