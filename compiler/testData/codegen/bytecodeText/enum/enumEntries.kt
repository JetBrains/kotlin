// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +EnumEntries

enum class MyEnum {
    E
}

// 1 INVOKEDYNAMIC
// 1 kotlin.enums.EnumEntries<MyEnum> getEntries\(\)
// 1 private final static synthetic Lkotlin/enums/EnumEntries; \$ENTRIES
// 1 public final static getEntries\(\)Lkotlin/enums/EnumEntries;
// 0 [^\$]entries
// 0 class [a-zA-Z]+\$EntriesMappings
