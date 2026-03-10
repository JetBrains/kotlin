// FILE: Anno.kt
annotation class Anno(val value: String)

// FILE: main.kt
@Anno(<expr>"13"</expr>)
class Foo
