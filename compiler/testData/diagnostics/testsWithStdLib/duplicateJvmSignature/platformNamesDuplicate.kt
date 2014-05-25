// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.platform.*

<!CONFLICTING_JVM_DECLARATIONS!>[platformName("bar")]
fun foo(a: Any)<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun bar(a: Any)<!> {}