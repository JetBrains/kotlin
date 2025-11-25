// RUN_PIPELINE_TILL: BACKEND
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

value class VC(val s: String)

@OptIn(ExperimentalNativeApi::class)
fun test(p: VC, p2: VC?, p3: Int, p4: Int?) {
    <!IDENTITY_HASH_CODE_ON_VALUE_TYPE!>p.identityHashCode()<!>
    <!IDENTITY_HASH_CODE_ON_VALUE_TYPE!>p2.identityHashCode()<!>
    <!IDENTITY_HASH_CODE_ON_VALUE_TYPE!>p3.identityHashCode()<!>
    <!IDENTITY_HASH_CODE_ON_VALUE_TYPE!>p4.identityHashCode()<!>

    with(p) { <!IDENTITY_HASH_CODE_ON_VALUE_TYPE!>identityHashCode()<!> }
}
