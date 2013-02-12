// "Change return type 'Foo<String>' to 'Foo<Int>' to match overridden method" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>Foo&lt;jet.Int&gt;</td></tr><tr><td>Found:</td><td>Foo&lt;jet.String&gt;</td></tr></table></html>
class Foo<T> {

}

open class A {
    open fun x() : Foo<Int> {
        return Foo<Int>();
    }
}

open class B : A() {
    override fun x() : Foo<Int> {
        return Foo<Int>();
    }
}

class C : B() {
    override fun x() : Foo<String><caret> {
        return Foo<String>();
    }
}