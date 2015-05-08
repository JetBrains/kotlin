// "Make A open" "true"
trait X {}
trait Y {}

class A {}
class B : X, A<caret>(), Y {}
