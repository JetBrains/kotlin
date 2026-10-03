// RUN_PIPELINE_TILL: CODEGEN
import kotlin.native.ref.*

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun foo(x: Int) {
    createCleaner(<!IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE!>42<!>) { println(x) }
}
