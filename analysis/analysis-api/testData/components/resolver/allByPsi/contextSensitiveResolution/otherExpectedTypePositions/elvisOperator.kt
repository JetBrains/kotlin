// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MyClass {
    object NestedInheritor : MyClass()

    companion object {
        val myClassProp: MyClass = MyClass()
        val stringProp: String = ""
        fun getNestedInheritor() = NestedInheritor
    }
}

fun <T>receive(e: T) {}
val ClassMemberAlias = MyClass.NestedInheritor

fun testElvis() {
    var i100: MyClass = NestedInheritor ?: myClassProp
    var i110: MyClass = NestedInheritor ?: stringProp
    var i120: MyClass = NestedInheritor ?: getNestedInheritor()

    receive<MyClass>(NestedInheritor ?: myClassProp)
    receive<MyClass>(NestedInheritor ?: stringProp)
    receive<MyClass>(NestedInheritor ?: getNestedInheritor())
}
