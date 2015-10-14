import kotlin.properties.ReadWriteProperty
import kotlin.properties.Delegates

class C {
    val `x$delegate`: ReadWriteProperty<Any, Any>? = null
    val x: String? by <!OPERATOR_MODIFIER_REQUIRED!>Delegates.notNull()<!>
}