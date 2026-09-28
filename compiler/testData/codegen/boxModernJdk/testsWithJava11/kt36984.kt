// SAM_CONVERSIONS: CLASS
// WITH_STDLIB

fun box(): String {
    val f = {}
    val sam = Runnable(f)
    val samJavaClass = sam::class.java

    if (samJavaClass.simpleName != "")
        throw Exception("samJavaClass.simpleName='${samJavaClass.simpleName}'")

    if (!samJavaClass.isAnonymousClass())
        throw Exception("!samJavaClass.isAnonymousClass(): '${samJavaClass.name}'")

    return "OK"
}
