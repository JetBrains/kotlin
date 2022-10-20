// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
val x: Int = 10
val Int.y get() = this

var Short.get: Long
    get() = 2
    set(value) {}