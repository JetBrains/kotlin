// ISSUE: KT-61065
// FILE: PrivateObjekt.kt

private object PrivateObjekt

// FILE: Main.kt

fun test(arg: Any?) {
    when (arg) {
        // K1: ok
        // K2: INVISIBLE_REFERENCE
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") PrivateObjekt -> Unit
    }

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") PrivateObjekt

    val it = @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") PrivateObjekt
}
