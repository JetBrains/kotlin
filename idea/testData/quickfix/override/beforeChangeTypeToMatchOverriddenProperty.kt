// "Change type 'Foo<String>' to 'Foo<Int>' to match overridden property" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>Foo&lt;jet.Int&gt;</td></tr><tr><td>Found:</td><td>Foo&lt;jet.String&gt;</td></tr></table></html>
class Foo<T> {

}

open class A {
    open var x : Foo<Int> = Foo<Int>();
}

open class B : A() {
    override var x : Foo<Int> = Foo<Int>();
}

class C : B() {
    override var x : Foo<String><caret> = Foo<String>();
}