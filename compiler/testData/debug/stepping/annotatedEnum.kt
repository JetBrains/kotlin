
// FILE: test.kt

annotation class Anno(val type: Type) {
    enum class Type { TYPE1, TYPE2 }
}

@Suppress("EnumEntryName")
@Anno(Anno.Type.TYPE1)
enum class Enum { ENTRY1, entry2 }

fun box(): String {
    Enum.ENTRY1
    Enum.valueOf("entry2")
    Enum.values()

    Anno.Type.TYPE2
    Anno.Type.valueOf("TYPE1")
    Anno.Type.values()
    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:10 <clinit>
// test.kt:14 box
// test.kt:15 box
// test.kt:17 box
// test.kt:5 <clinit>
// test.kt:18 box
// test.kt:19 box
// test.kt:20 box


// EXPECTATIONS NATIVE
// test.kt:13 box
// test.kt:8 $getEnumAt
// test.kt:1 $getEnumAt
// test.kt:8 $getEnumAt
// test.kt:10 $getEnumAt
// test.kt:13 box
// test.kt:14 box
// test.kt:1 valueOf
// test.kt:1 valueOf
// test.kt:14 box
// test.kt:15 box
// test.kt:1 values
// test.kt:1 values
// test.kt:15 box
// test.kt:17 box
// test.kt:5 $getEnumAt
// test.kt:1 $getEnumAt
// test.kt:5 $getEnumAt
// test.kt:17 box
// test.kt:18 box
// test.kt:1 valueOf
// test.kt:1 valueOf
// test.kt:18 box
// test.kt:19 box
// test.kt:1 values
// test.kt:1 values
// test.kt:19 box
// test.kt:20 box
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:10 Enum_initEntries
// test.kt:8 <init>
// test.kt:10 Enum_initEntries
// test.kt:8 <init>
// test.kt:14 box
// test.kt:15 box
// test.kt:17 box
// test.kt:5 Type_initEntries
// test.kt:5 <init>
// test.kt:5 Type_initEntries
// test.kt:5 <init>
// test.kt:18 box
// test.kt:19 box
// test.kt:20 box

// EXPECTATIONS WASM
// test.kt:13 $box (9)
// test.kt:10 $Enum_initEntries (18)
// test.kt:10 $Enum.<init> (34)
// test.kt:10 $Enum_initEntries (18, 26)
// test.kt:10 $Enum.<init> (34)
// test.kt:10 $Enum_initEntries (26)
// test.kt:13 $box (9)
// test.kt:14 $box (17, 9)
// test.kt:10 $Enum_initEntries (18)
// test.kt:14 $box (9)
// test.kt:15 $box (9)
// test.kt:10 $Enum_initEntries (18)
// test.kt:15 $box (9)
// test.kt:17 $box (14)
// test.kt:5 $Type_initEntries (22)
// test.kt:5 $Type.<init> (36)
// test.kt:5 $Type_initEntries (22, 29)
// test.kt:5 $Type.<init> (36)
// test.kt:5 $Type_initEntries (29)
// test.kt:17 $box (14)
// test.kt:18 $box (22, 14)
// test.kt:5 $Type_initEntries (22)
// test.kt:18 $box (14)
// test.kt:19 $box (14)
// test.kt:5 $Type_initEntries (22)
// test.kt:19 $box (14)
// test.kt:20 $box (11, 4)
