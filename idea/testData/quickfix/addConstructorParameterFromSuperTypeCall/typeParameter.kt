// "Add constructor parameter 'x'" "true"
abstract class Foo<T>(x: T)
class Boo : Foo<String>(<caret>)
