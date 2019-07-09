fun WithCompanion.test(): String {
    object : WithCompanion(this) {}
    return "OK"
}

open class WithCompanion(a: WithCompanion.Companion) {
    companion object
}

fun box(): String {
    return WithCompanion(WithCompanion.Companion).test()
}
