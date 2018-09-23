// "Add 'open val hoge: Int' to 'Foo'" "true"
open class Foo

class Bar: Foo() {
    override<caret> val hoge = 3
}