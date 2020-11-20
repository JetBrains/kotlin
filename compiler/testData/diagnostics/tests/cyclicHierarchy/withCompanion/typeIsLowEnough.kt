// FIR_IDENTICAL
// see https://youtrack.jetbrains.com/issue/KT-21515

abstract class DerivedAbstract : C.Base() {
    override abstract fun m()
}

public class C {
    class Data

    open class Base () {
        open fun m() {}
    }

    // Note that Data is resolved successfully here because we don't step on error-scope
    val data: Data = Data()

    companion object : DerivedAbstract() {
        override fun m() {}
    }
}