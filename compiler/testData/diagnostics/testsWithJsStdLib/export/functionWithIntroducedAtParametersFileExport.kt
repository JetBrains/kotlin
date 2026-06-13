// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
@file:OptIn(ExperimentalVersionOverloading::class)
@file:JsExport

fun withIntroducedAt(
    x: Int,
    @IntroducedAt("1") y: Int = x,
    @IntroducedAt("1") ok1: String = "OK",
    @IntroducedAt("2") ok2: String = ok1
) {}

fun nonAscendingVersion(
    x: Int,
    @IntroducedAt("2") ok: String = "OK",
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> y: Int = x
) {}

fun invalidParameterPosition(
    x: Int,
    @IntroducedAt("1") y: Int = x,
    <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>z: Int<!>
) {}

fun invalidDependency(
    @IntroducedAt("2") x: Int = 42,
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> y: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY!>x<!>
) {}
