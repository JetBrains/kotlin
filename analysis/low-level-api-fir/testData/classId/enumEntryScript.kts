package one

/* ClassId: one/MyEnum [PsiFqName: one.EnumEntryScript.MyEnum] */enum class MyEnum {
    /* ClassId: null [PsiFqName: one.EnumEntryScript.MyEnum.Entry] */Entry {
        fun foo() = Unit
        /* ClassId: null [PsiFqName: one.EnumEntryScript.MyEnum.Entry.ClassInsideEntry] */class ClassInsideEntry
    },
    /* ClassId: null [PsiFqName: one.EnumEntryScript.MyEnum.Entry2] */Entry2,
    /* ClassId: null [PsiFqName: one.EnumEntryScript.MyEnum.Entry3] */Entry3 {

    }
}

// IGNORE_CONSISTENCY_CHECK: KtEnumEntry have FqName, but doesn't have ClassId
