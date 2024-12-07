package one

open class ClassWithParameter(s: String)

class TopLevelClass : ClassWithParameter("${
    {
        fun s<caret>tr(): String = 42
        str()
    }()
}")