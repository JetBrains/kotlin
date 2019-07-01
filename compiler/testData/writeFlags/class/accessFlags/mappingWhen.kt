enum class BigEnum {
    ITEM1,
    ITEM2
}

fun bar1(x : BigEnum) : String {
    when (x) {
        BigEnum.ITEM1 -> return "123"
        BigEnum.ITEM2-> return "456"
    }

    return "-1";

}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: MappingWhenKt$WhenMappings
// FLAGS: ACC_PUBLIC, ACC_SYNTHETIC, ACC_FINAL, ACC_SUPER
