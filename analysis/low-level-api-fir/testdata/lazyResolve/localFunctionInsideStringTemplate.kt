package one

open class ClassWithParameter(s: String)

class TopLevelClass : ClassWithParameter("${
    fu<caret>n(): String {
        return ""
    }()
}")