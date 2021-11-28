// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// SKIP_JDK6

import java.lang.annotation.Documented

annotation class NoDocumented

@MustBeDocumented
annotation class ExplicitMustBeDocumented

@Documented
annotation class ExplicitJavaDocumented

@MustBeDocumented
@Documented
annotation class ExplicitBoth

inline fun <reified A> isDocumented(): Boolean =
    A::class.java.getDeclaredAnnotation(Documented::class.java) != null

fun box(): String {
    if (isDocumented<NoDocumented>()) return "Fail NoDocumented"
    if (!isDocumented<ExplicitMustBeDocumented>()) return "Fail ExplicitMustBeDocumented"
    if (!isDocumented<ExplicitJavaDocumented>()) return "Fail ExplicitJavaDocumented"
    if (!isDocumented<ExplicitBoth>()) return "Fail ExplicitBoth"

    return "OK"
}
