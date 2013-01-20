// "Add 'open' modifier to supertype" "true"
trait X {}
trait Y {}

open class A {}
class B : X, A<caret>(), Y {}
