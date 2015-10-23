open class Static(): IStatic {
    val p = IStatic::class.java.getDeclaredField("const").get(null)
}

interface IStatic {
    fun doSth() {
    }

    companion object : Static()  {
        const val const = 1;
    }
}

fun box(): String {
    IStatic.doSth()

    val companion: Any? = CompanionInitialization.getCompanion()
    if (companion == null) return "fail 1"
    if (companion != IStatic) return "fail 2"

    return "OK"
}