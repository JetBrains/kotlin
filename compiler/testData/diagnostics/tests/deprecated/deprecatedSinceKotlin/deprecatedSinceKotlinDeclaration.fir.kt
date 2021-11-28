// !SKIP_JAVAC
package kotlin.sub

@Deprecated("", ReplaceWith(""))
@DeprecatedSinceKotlin(warningSince = "1.0", errorSince = "1.1", hiddenSince = "1.2")
fun good() {}

<!DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS!>@<!DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED!>DeprecatedSinceKotlin<!>()<!>
class Clazz

@Deprecated("", level = DeprecationLevel.WARNING)
<!DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS!>@<!DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL!>DeprecatedSinceKotlin<!>()<!>
fun fooWarning() {}

@Deprecated("", ReplaceWith(""), DeprecationLevel.WARNING)
<!DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS!>@<!DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL!>DeprecatedSinceKotlin<!>()<!>
fun fooDefaultWarning() {}

@Deprecated("", level = DeprecationLevel.ERROR)
<!DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS!>@<!DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL!>DeprecatedSinceKotlin<!>()<!>
fun fooError() {}

@Deprecated("", level = DeprecationLevel.HIDDEN)
<!DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS!>@<!DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL!>DeprecatedSinceKotlin<!>()<!>
fun fooHidden() {}

@Deprecated("")
<!DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS!>@DeprecatedSinceKotlin(warningSince = "1.1", errorSince = "1.0")<!>
fun fooWarningIsGreater1() {}

@Deprecated("")
<!DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS!>@DeprecatedSinceKotlin(warningSince = "1.1", hiddenSince = "1.0")<!>
fun fooWarningIsGreater2() {}

@Deprecated("")
<!DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS!>@DeprecatedSinceKotlin(warningSince = "1.1", errorSince = "1.3", hiddenSince = "1.2")<!>
fun fooErrorIsGreater() {}

@Deprecated("")
<!DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS!>@DeprecatedSinceKotlin("1.2", "1.1", "1.1")<!>
fun fooDefault() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.1", "1.1", "1.1")
fun fooEqual() {}
