// KT-91574: the delegate initializer is inlined into `<clinit>` of the object, which has no line numbers.
// The marker of `provided` must still carry the line of the call.
import kotlin.properties.ReadOnlyProperty

inline fun <T> provided(key: String, default: T): ReadOnlyProperty<Any?, T> {
    val k = key.length
    return ReadOnlyProperty { _, _ -> if (k > 0) default else default }
}

object Cfg {
    val enabled: Boolean by provided("a", default = true)
}

// JVM_IR_TEMPLATES
// 2 LOCALVARIABLE \$i\$f\$provided I

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE \$i\$f\$provided I
// 1 LOCALVARIABLE \$i\$f\$provided\\1\\11 I
