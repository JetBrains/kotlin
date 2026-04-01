// FILE: Properties.kt
@Anno
internal var withCustomSetter = "OK"
    set(value) {}

@Anno
internal var withCustomGetter = "OK"
    get() = "KO"

@Anno
internal var custom = "OK"
    get() = field
    set(value) {
        field = value
    }

@Anno
internal var variable = "OK"

// FILE: Anno.kt
annotation class Anno