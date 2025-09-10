// WITH_STDLIB
// LANGUAGE: +PackagePrivateFileClassesWithAllPrivateMembers
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR

private fun f() {
}

private val a = Unit
private val b by lazy { Unit }

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
public fun g() {
}
