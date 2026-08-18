// WITH_STDLIB
// TARGET_PLATFORM: JVM

@JvmInline
value class Some(val value: String)

var topLevelProp: Some = Some("1")
var Some.topLevelPropInExtension: Int
    get() = 1
    set(value) {}

fun topLevelFunInReturn(): Some = Some("1")
fun topLevelFunInParameter(s: Some) {}
fun Some.topLevelFunInExtension() {}

@JvmName("specialName")
fun topLevelFunWithJvmName(s: Some) {}

fun funWithResultInParameter(r: Result<Int>) {}

fun <T : Some> funWithValueClassTypeParameter(t: T) {}

// An implicit type is not resolved on purpose, so mangling is not detected for the declarations below
var topLevelPropImplicit = Some("1")
fun topLevelFunInImplicitReturn() = Some("1")

class RegularClass {
    var classProp: Some = Some("1")
    var Some.classPropInExtension: Int
        get() = 1
        set(value) {}

    // Only the getter is mangled, as 'kotlin.Result' mangles a return type, but not a parameter type
    var resultProp: Result<Int> = Result.success(1)

    fun classFunInReturn(): Some = Some("1")
    fun classFunInParameter(s: Some) {}
    fun Some.classFunInExtension() {}

    @JvmName("specialName")
    fun classFunWithJvmName(s: Some) {}

    internal fun internalClassFunInParameter(s: Some) {}

    fun funWithResultInReturn(): Result<Int> = Result.success(1)

    // An implicit type is not resolved on purpose, so mangling is not detected for the declarations below
    var classPropImplicit = Some("1")
    fun classFunInImplicitReturn() = Some("1")
}
