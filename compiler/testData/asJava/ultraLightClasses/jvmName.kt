/** should load cls */
class C {
    var rwProp: Int
        @JvmName("get_rwProp")
        get() = 0
        @JvmName("set_rwProp")
        set(v) {}

    fun getRwProp(): Int = 123
    fun setRwProp(v: Int) {}
}
