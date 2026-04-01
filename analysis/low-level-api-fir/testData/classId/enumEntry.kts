package one

/* ClassId: one/MyEnum [PsiFqName: one.EnumEntry.MyEnum] */enum class MyEnum {
    /* ClassId: null [PsiFqName: one.EnumEntry.MyEnum.Entry] */Entry {
        fun foo() = Unit
        /* ClassId: null [PsiFqName: one.EnumEntry.MyEnum.Entry.ClassInsideEntry] */class ClassInsideEntry
    },
    /* ClassId: null [PsiFqName: one.EnumEntry.MyEnum.Entry2] */Entry2,
    /* ClassId: null [PsiFqName: one.EnumEntry.MyEnum.Entry3] */Entry3 {

    }
}

// IGNORE_CONSISTENCY_CHECK: KtEnumEntry have FqName, but doesn't have ClassId
