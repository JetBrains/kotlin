// WITH_STDLIB

class MyClass constructor(val s: String = "OK", @IntroducedAt("2") val property: UInt = 2u)

fun box(): String = MyClass().s
