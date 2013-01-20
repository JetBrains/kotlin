// "Add 'open' modifier to supertype" "true"
trait X {}
trait Y {}

class A {}
class B : X, A<caret>(), Y {}
