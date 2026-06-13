// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
@file:OptIn(ExperimentalVersionOverloading::class)

@JsExport
data class ConstructorVersioning(
    val x: Int,
    @IntroducedAt("1") val y: Int = x,
    @IntroducedAt("1") val ok1: String = "OK",
    @IntroducedAt("2") val ok2: String = ok1
)

@JsExport
data class ConstructorWithNonAscendingVersion(
    val x: Int,
    @IntroducedAt("2") val ok: String = "OK",
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> val y: Int = x
)

@JsExport
data class ConstructorWithInvalidParameterPosition(
    val x: Int,
    @IntroducedAt("1") val y: Int = x,
    <!INVALID_NON_OPTIONAL_PARAMETER_POSITION!>val z: Int<!>
)

@JsExport
data class ConstructorWithInvalidDependency(
    @IntroducedAt("2") val x: Int = 42,
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> val y: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY!>x<!>
)
