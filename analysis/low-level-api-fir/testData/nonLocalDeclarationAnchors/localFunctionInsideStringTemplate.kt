package one

open class ClassWithParameter(s: String)

class TopLevelClass : ClassWithParameter("${
    fun(): String {
        return ""
    }()
}")