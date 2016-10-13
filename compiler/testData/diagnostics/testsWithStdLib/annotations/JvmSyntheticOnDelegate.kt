// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.properties.Delegates

class My {
    <!JVM_SYNTHETIC_ON_DELEGATE!>@delegate:JvmSynthetic<!> val s: String by lazy { "s" }

    // Both Ok
    @get:JvmSynthetic val t: String by lazy { "t" }
    @set:JvmSynthetic var z: String by Delegates.observable("?") { prop, old, new -> old.hashCode() }
}
