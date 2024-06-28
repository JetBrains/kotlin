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

fun reso<caret>lveMe() {
    class LocalClass : AnotherTopLevelClass() {
        override fun foo(i: Int) {
        }

        class NestedLocalClass
    }
}