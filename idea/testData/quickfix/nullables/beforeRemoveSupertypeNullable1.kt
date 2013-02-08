// "Remove supertype '?'" "true"
open class Foo() {}
class Bar() : Foo?<caret>() {}