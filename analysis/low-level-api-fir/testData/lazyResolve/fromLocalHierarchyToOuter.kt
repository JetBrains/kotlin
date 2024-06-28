open class TopLevelClass {
    open fun foo(i: Int) {

    }

    open fun boo(b: String) {

    }
}

open class AnotherTopLevelClass : TopLevelClass() {
    override fun foo(i: Int) {

    }
}

fun resolve<caret>Me() {
    open class LocalClass : AnotherTopLevelClass() {
        override fun foo(i: Int) {
        }
    }

    class SecondLocalClass : LocalClass() {
        override fun boo(b: String) {
        }
    }
}