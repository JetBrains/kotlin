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

val ClassMemberAlias = MyClass.NestedInheritor

fun <T>receive(e: T) {}
fun <T> run(b: () -> T): T = b()

fun testIfElse(i: Int) {
    val s: MyClass =
        if (i == 0) {
            NestedInheritor
            NestedInheritor
        }
        else if (i == 1) {
            myClassProp
            myClassProp
        }
        else ClassMemberAlias

    val s2: MyClass =
        if (i == 2) {
            stringProp
            stringProp
        }
        else ClassMemberAlias

    receive<MyClass>(
        if (i == 0) {
            NestedInheritor
            NestedInheritor
            // KT-76400
        }
        else if (i == 1) {
            myClassProp
            myClassProp
            // KT-76400
        }
        else if (i == 2) {
            getNestedInheritor()
            getNestedInheritor()
        }
        else if (i == 3) {
            stringProp
            stringProp
            // KT-76400
        }
        else ClassMemberAlias
    )

    run<MyClass> {
        if (i == 0) {
            NestedInheritor
            NestedInheritor
            // KT-76400
        }
        else if (i == 1) {
            myClassProp
            myClassProp
            // KT-76400
        }
        else ClassMemberAlias
    }
}
