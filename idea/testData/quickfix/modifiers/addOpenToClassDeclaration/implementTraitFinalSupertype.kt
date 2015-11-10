// "Make 'A' open" "true"
interface X {}
interface Y {}

class A {}
class B : X, A<caret>(), Y {}
