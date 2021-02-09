// !WITH_NEW_INFERENCE
import com.unknown

fun ff() {
    val a = <!UNRESOLVED_REFERENCE!>unknown<!>()
    val b = a?.plus(42)
}