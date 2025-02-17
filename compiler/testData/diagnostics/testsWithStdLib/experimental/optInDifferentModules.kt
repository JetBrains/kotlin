// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1
// FILE: m1.kt

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class UnsupportedAppVersion

annotation class UnusedSince(val version: AppVersion)

enum class AppVersion {
    @UnsupportedAppVersion V0_1_0,
    @UnsupportedAppVersion V0_2_0,
    @UnsupportedAppVersion V0_2_5 {
        override fun toString() = "2.5"
    },
    V0_3_0,
}

@OptIn(UnsupportedAppVersion::class) fun something(appVersion: AppVersion): String {
    if (appVersion <= AppVersion.V0_2_0) {
        return "something special"
    }

    return "foo"
}

@UnsupportedAppVersion
fun foo() {}

@UnsupportedAppVersion
val x = 2

// MODULE: m2(m1)
// FILE: m2.kt

class MyDto(
    val property1: Int,
    @UnusedSince(AppVersion.<!OPT_IN_USAGE_ERROR!>V0_2_0<!>) val property2: Int,
    @UnusedSince(AppVersion.<!OPT_IN_USAGE_ERROR!>V0_2_5<!>) val property3: Int,
)

fun bar() {
    <!OPT_IN_USAGE_ERROR!>foo<!>()
    val y = <!OPT_IN_USAGE_ERROR!>x<!>
}
