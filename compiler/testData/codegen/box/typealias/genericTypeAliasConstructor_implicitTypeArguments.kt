// WITH_STDLIB
class Cell<T>(val x: T)

typealias AliasedCell<TT> = Cell<TT>

class MyClass

val propertyWithImplicitType = AliasedCell(MyClass())

fun box(): String = "OK"