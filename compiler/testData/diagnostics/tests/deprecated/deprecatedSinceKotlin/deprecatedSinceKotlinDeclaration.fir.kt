// !SKIP_JAVAC
package kotlin.sub

@Deprecated("", ReplaceWith(""))
@DeprecatedSinceKotlin(warningSince = "1.0", errorSince = "1.1", hiddenSince = "1.2")
fun good() {}

@DeprecatedSinceKotlin()
class Clazz

@Deprecated("", level = DeprecationLevel.WARNING)
@DeprecatedSinceKotlin()
fun fooWarning() {}

@Deprecated("", ReplaceWith(""), DeprecationLevel.WARNING)
@DeprecatedSinceKotlin()
fun fooDefaultWarning() {}

@Deprecated("", level = DeprecationLevel.ERROR)
@DeprecatedSinceKotlin()
fun fooError() {}

@Deprecated("", level = DeprecationLevel.HIDDEN)
@DeprecatedSinceKotlin()
fun fooHidden() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.1", errorSince = "1.0")
fun fooWarningIsGreater1() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.1", hiddenSince = "1.0")
fun fooWarningIsGreater2() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.1", errorSince = "1.3", hiddenSince = "1.2")
fun fooErrorIsGreater() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.2", "1.1", "1.1")
fun fooDefault() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.1", "1.1", "1.1")
fun fooEqual() {}
