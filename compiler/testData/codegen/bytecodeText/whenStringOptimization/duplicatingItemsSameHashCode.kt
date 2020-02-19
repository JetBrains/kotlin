// IGNORE_BACKEND: JVM_IR
// TODO KT-36846 Generate TABLESWITCH/LOOKUPSWITCH in case of matching hashCodes in JVM_IR

fun foo(x : String) : String {
    assert("abz]".hashCode() == "aby|".hashCode())

    when (x) {
        "abz]" -> return "abz"
        "ghi"  -> return "ghi"
        "aby|" -> return "aby"
        "abz]" -> return "fail"
    }

    return "other"
}

// 1 LOOKUPSWITCH
