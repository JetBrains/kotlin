// TARGET_BACKEND: JVM

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

fun check(body: () -> Unit, signature: String): String? {
    try {
        body()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != signature) return "Fail $signature: " + e.message
    }

    return null
}

fun box(): String {
    return check({defaultGetter}, "NativePropertyAccessorsKt.getDefaultGetter()I")
           ?: check({defaultSetter = 1}, "NativePropertyAccessorsKt.setDefaultSetter(I)V")

           ?: check({C.defaultGetter}, "C\$Companion.getDefaultGetter()I")
           ?: check({C.defaultSetter = 1}, "C\$Companion.setDefaultSetter(I)V")

           ?: check({C().defaultGetter}, "C.getDefaultGetter()I")
           ?: check({C().defaultSetter = 1}, "C.setDefaultSetter(I)V")

           ?: "OK"
}
