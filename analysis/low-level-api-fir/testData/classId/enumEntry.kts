package one

/* ClassId: one/MyEnum */enum class MyEnum {
    /* ClassId: null */Entry {
        fun foo() = Unit
        /* ClassId: null */class ClassInsideEntry
    },
    /* ClassId: null */Entry2,
    /* ClassId: null */Entry3 {

    }
}

// IGNORE_CONSISTENCY_CHECK: KtEnumEntry have FqName, but doesn't have ClassId
