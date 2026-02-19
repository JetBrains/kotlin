package one

/* ClassId: one/ClassWithParameter */open class ClassWithParameter(i: () -> Unit)

/* ClassId: one/TopLevelClass */class TopLevelClass : ClassWithParameter({
    /* ClassId: null */class LocalClass
})
