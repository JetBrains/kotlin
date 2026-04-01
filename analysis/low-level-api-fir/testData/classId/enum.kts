package one

/* ClassId: one/MyEnum [PsiFqName: one.Enum.MyEnum] */enum class MyEnum {
    ;

    /* ClassId: one/MyEnum.InsideEnum [PsiFqName: one.Enum.MyEnum.InsideEnum] */class InsideEnum
    /* ClassId: one/MyEnum.ObjectInsideEnum [PsiFqName: one.Enum.MyEnum.ObjectInsideEnum] */object ObjectInsideEnum {
        /* ClassId: one/MyEnum.ObjectInsideEnum.Nested [PsiFqName: one.Enum.MyEnum.ObjectInsideEnum.Nested] */typealias Nested
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26902
