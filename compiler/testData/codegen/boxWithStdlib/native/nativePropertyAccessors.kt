class C {
    default object {
        val defaultGetter: Int = 1
            [native] get

        var defaultSetter: Int = 1
            [native] get
            [native] set
    }

    val defaultGetter: Int = 1
        [native] get

    var defaultSetter: Int = 1
        [native] get
        [native] set
}

val defaultGetter: Int = 1
    [native] get

var defaultSetter: Int = 1
    [native] get
    [native] set

fun check(body: () -> Unit, signature: String): String? {
    try {
        body()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.getMessage() != signature) return "Fail $signature: " + e.getMessage()
    }

    return null
}

fun box(): String {
    return check({defaultGetter}, "_DefaultPackage.getDefaultGetter()I")
           ?: check({defaultSetter = 1}, "_DefaultPackage.setDefaultSetter(I)V")

           ?: check({C.defaultGetter}, "C\$Default.getDefaultGetter()I")
           ?: check({C.defaultSetter = 1}, "C\$Default.setDefaultSetter(I)V")

           ?: check({C().defaultGetter}, "C.getDefaultGetter()I")
           ?: check({C().defaultSetter = 1}, "C.setDefaultSetter(I)V")

           ?: "OK"
}