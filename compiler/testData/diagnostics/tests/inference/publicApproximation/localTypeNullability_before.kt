// LANGUAGE: -KeepNullabilityWhenApproximatingLocalType
// ISSUE: KT-53982

interface I

val condition = false

<!APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE!>fun foo4()<!>  = if (condition) object : I {} else null

fun bar1() = foo4().toString()

fun bar2() = foo4()<!UNNECESSARY_SAFE_CALL!>?.<!>toString()

object J {
    fun <T> id(x: T): T? {
        return null
    }
}

<!APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE!>fun foo5()<!> = J.id(object : I {})

fun bar3() = foo5()<!UNNECESSARY_SAFE_CALL!>?.<!>toString()