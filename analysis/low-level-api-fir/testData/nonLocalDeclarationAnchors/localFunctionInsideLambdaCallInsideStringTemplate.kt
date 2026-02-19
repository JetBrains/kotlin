package one

open class ClassWithParameter(s: String)

class TopLevelClass : ClassWithParameter("${
    {
        fun str(): String = 42
        str()
    }()
}")