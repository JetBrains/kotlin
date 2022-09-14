@JvmInline
value class Some(val value: String)

var topLevelProp: Some = Some("1")
var Some.topLevelPropInExtension: Int
    get() = 1
    set(value) {}

fun topLevelFunInReturn(): Some = Some("1")
fun topLevelFunInParameter(s: Some) {}
fun Some.topLevelFunInExtension() {}

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
