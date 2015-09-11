package test

annotation class JustString(val string: String)

annotation class StringArray(val stringArray: Array<String>)

@JustString("kotlin")
@StringArray(arrayOf())
class C1

@StringArray(arrayOf("java", ""))
class C2
