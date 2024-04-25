// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

fun <T> select(vararg x: T) = x[0]

fun main() {
    val x1: String.() -> String = if (true) {{ this }} else {{ this }}
    val x2: String.() -> String = if (true) {{ -> this }} else {{ -> this }}
    val x3: () -> String = if (true) {{ -> "this" }} else {{ -> "this" }}
    val x4: String.() -> String = if (true) <!TYPE_MISMATCH!>{{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str: String<!> -> "this" }}<!> else <!TYPE_MISMATCH!>{{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str: String<!> -> "this" }}<!>
    val x41: String.(String) -> String = if (true) <!TYPE_MISMATCH!>{{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str: String, str2: String<!> -> "this" }}<!> else <!TYPE_MISMATCH!>{{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str: String, str2: String<!> -> "this" }}<!>
    val x42: String.(String) -> String = if (true) <!TYPE_MISMATCH!>{{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!><!> -> "this" }}<!> else <!TYPE_MISMATCH!>{{ <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str, <!CANNOT_INFER_PARAMETER_TYPE!>str2<!><!> -> "this" }}<!>
    val x5: String.() -> String = if (true) <!TYPE_MISMATCH!>{{ <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str<!> -> "this" }}<!> else <!TYPE_MISMATCH!>{{ <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str<!> -> "this" }}<!>
    val x6: String.() -> String = if (true) <!TYPE_MISMATCH!>{{ <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>str<!> -> "this" }}<!> else {{ "this" }}
    val x7: String.() -> String = select({ -> this }, { -> this })
    val x8: String.() -> String = select({ this }, { this })
}
