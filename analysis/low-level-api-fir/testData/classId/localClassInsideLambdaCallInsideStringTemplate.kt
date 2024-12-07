package one

/* ClassId: one/ClassWithParameter */open class ClassWithParameter(s: String)

/* ClassId: one/TopLevelClass */class TopLevelClass : ClassWithParameter("${
    {
        /* ClassId: null */class F
        F()
    }()
}")
