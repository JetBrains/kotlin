// FILE: test.kt

class AWithCompanion {
    companion object {
        //Comment before
        val compPropVal = 1
    }
}

fun box() {
    AWithCompanion.compPropVal
}

// EXPECTATIONS JVM JVM_IR
// test.kt:11 box
// test.kt:6 <clinit>
// test.kt:6 getCompPropVal
// test.kt:6 getCompPropVal
// test.kt:11 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:6 <init>
// test.kt:4 <init>
// test.kt:12 box
