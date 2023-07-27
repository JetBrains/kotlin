package one

interface Interface {
    fun foo(param: String)
}

open class ClassWithParameter(i: Interface)

class TopLevelClass : ClassWithParameter(object : Interface {
    override fun fo<caret>o(param: String) {
    }
})