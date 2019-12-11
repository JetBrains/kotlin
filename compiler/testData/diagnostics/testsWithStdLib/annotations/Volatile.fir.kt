// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
import kotlin.jvm.Volatile
import kotlin.properties.Delegates

class My {
    @Volatile val x = 0
    // ok
    @Volatile var y = 1

    @delegate:Volatile var z: String by Delegates.observable("?") { prop, old, new -> old.hashCode() }

    @field:Volatile val w = 2
}
