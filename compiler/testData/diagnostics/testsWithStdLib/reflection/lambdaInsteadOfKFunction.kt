// RUN_PIPELINE_TILL: FRONTEND
// WITH_REFLECT
// ISSUE: KT-69991

import kotlin.reflect.KFunction0

fun compose(): KFunction0<String> {
    return <!TYPE_MISMATCH!>{ "" }<!>
}
