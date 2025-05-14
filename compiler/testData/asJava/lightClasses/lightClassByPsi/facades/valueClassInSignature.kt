// LIBRARY_PLATFORMS: JVM

@JvmInline
value class Some(val value: String)

var topLevelProp: Some = Some("1")
var Some.topLevelPropInExtension: Int
    get() = 1
    set(value) {}

fun topLevelFunInReturn(): Some = Some("1")
fun topLevelFunInParameter(s: Some) {}
fun Some.topLevelFunInExtension() {}

@JvmOverloads
fun withJvmOverloads(regularParameter: Int = 0, valueClassParameter: Some = Some("str")) {

}

@JvmOverloads
fun withJvmOverloadsButWithoutDefault(valueClassParameter: Some, regularParameter: Int = 0) {

}

@JvmOverloads
fun withJvmOverloadsInDifferentPositions(first: Int = 0, second: Some = Some("1"), third: Int = 2, fourth: Some = Some("3")) {

}

@JvmOverloads
@JvmName("specialName")
fun withJvmOverloadsAndJvmName(first: Int = 0, second: Some = Some("1"), third: Int = 2, fourth: Some = Some("3")) {

}

@JvmOverloads
fun Some.withJvmOverloadsAndValueReceiver(regularParameter: Int = 0, valueClassParameter: Some = Some("str")) {

}

class SomeClass {
    var memberProp: Some = Some("1")
    var Some.memberPropInExtension: Int
        get() = 1
        set(value) {}

    fun memberFunInReturn(): Some = Some("1")
    fun memberFunInParameter(s: Some) {}
    fun Some.memberFunInExtension() {}
}

interface SomeInterface {
    var memberProp: Some
    var Some.memberPropInExtension: Int
        get() = 1
        set(value) {}

    fun memberFunInReturn(): Some
    fun memberFunInParameter(s: Some)
    fun Some.memberFunInExtension()
}
