// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

value class Some(val value: String, val count: Int)

var topLevelProp: Some = Some("1", 1)
var Some.topLevelPropInExtension: Int
    get() = 1
    set(value) {}

fun topLevelFunInReturn(): Some = Some("1", 1)
fun topLevelFunInParameter(s: Some) {}
fun Some.topLevelFunInExtension() {}

@JvmOverloads
fun withJvmOverloads(regularParameter: Int = 0, valueClassParameter: Some = Some("str", 1)) {

}

@JvmOverloads
fun withJvmOverloadsButWithoutDefault(valueClassParameter: Some, regularParameter: Int = 0) {

}

@JvmOverloads
fun withJvmOverloadsInDifferentPositions(first: Int = 0, second: Some = Some("1", 1), third: Int = 2, fourth: Some = Some("3", 3)) {

}

@JvmOverloads
@JvmName("specialName")
fun withJvmOverloadsAndJvmName(first: Int = 0, second: Some = Some("1", 1), third: Int = 2, fourth: Some = Some("3", 3)) {

}

@JvmOverloads
fun Some.withJvmOverloadsAndValueReceiver(regularParameter: Int = 0, valueClassParameter: Some = Some("str", 1)) {

}
