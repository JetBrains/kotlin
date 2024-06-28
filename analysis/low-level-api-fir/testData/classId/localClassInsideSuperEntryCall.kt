package one

/* ClassId: one/Interface */interface Interface {
    fun foo(param: String)
}

/* ClassId: one/ClassWithParameter */open class ClassWithParameter(i: Interface)

/* ClassId: one/TopLevelClass */class TopLevelClass : ClassWithParameter(/* ClassId: null */object : Interface {
    /* ClassId: null */class NestedClassFromAnonymousObject
})
