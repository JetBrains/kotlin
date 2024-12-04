// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58665
// FULL_JDK

import java.util.*

fun use(x: String?) {
    Optional.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}