sealed class X {
    internal abstract var /*rename*/overridableVar: Int

    abstract class Y : X() {
        override var overridableVar: Int
            get() = 1
            set(value) {}
    }

    class Z : Y() {
        override var overridableVar: Int
            get() = super.overridableVar
            set(value) { super.overridableVar = value }
    }
}

fun get() : X? {
    return X.Z()
}

fun test() {
    get()?.overridableVar
    get()?.overridableVar = 3
}
