package one

/* ClassId: one/MyEnum */enum class MyEnum {
    /* ClassId: null [PsiFqName: one.MyEnum.Entry] */Entry {
        fun foo() = Unit
        /* ClassId: null [PsiFqName: one.MyEnum.Entry.ClassInsideEntry] */class ClassInsideEntry
    },
    /* ClassId: null [PsiFqName: one.MyEnum.Entry2] */Entry2,
    /* ClassId: null [PsiFqName: one.MyEnum.Entry3] */Entry3 {

    }
}

// IGNORE_CONSISTENCY_CHECK: KtEnumEntry have FqName, but doesn't have ClassId