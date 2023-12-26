// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// JVM_ABI_K1_K2_DIFF: KT-63984

// FULL_JDK

class C {
    companion object {
        val defaultGetter: Int = 1
            external get

        var defaultSetter: Int = 1
            external get
            external set
    }

    val defaultGetter: Int = 1
        external get

    var defaultSetter: Int = 1
        external get
        external set
}

val defaultGetter: Int = 1
    external get

var defaultSetter: Int = 1
    external get
    external set

fun check(body: () -> Unit, signature: String, jdk11Signature: String): String? {
    try {
        body()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != signature && e.message != jdk11Signature) return "Fail $signature: " + e.message
    }

    return null
}

fun box(): String {
    return check({defaultGetter}, "NativePropertyAccessorsKt.getDefaultGetter()I", "'int NativePropertyAccessorsKt.getDefaultGetter()'")
           ?: check({defaultSetter = 1}, "NativePropertyAccessorsKt.setDefaultSetter(I)V", "'void NativePropertyAccessorsKt.setDefaultSetter(int)'")

           ?: check({C.defaultGetter}, "C\$Companion.getDefaultGetter()I", "'int C\$Companion.getDefaultGetter()'")
           ?: check({C.defaultSetter = 1}, "C\$Companion.setDefaultSetter(I)V", "'void C\$Companion.setDefaultSetter(int)'")

           ?: check({C().defaultGetter}, "C.getDefaultGetter()I", "'int C.getDefaultGetter()'")
           ?: check({C().defaultSetter = 1}, "C.setDefaultSetter(I)V", "'void C.setDefaultSetter(int)'")

           ?: "OK"
}
