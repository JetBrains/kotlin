package one

open class ClassWithParameter(i: () -> Unit)

class TopLevelClass : ClassWithParameter({
    fun foo(param: String) {
    }
})