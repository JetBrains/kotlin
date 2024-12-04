package one

open class ClassWithParameter(i: () -> Unit)

class TopLevelClass : ClassWithParameter({
    fun fo<caret>o(param: String) {
    }
})