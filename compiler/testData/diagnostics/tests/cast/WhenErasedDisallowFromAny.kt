
import java.util.List;

fun ff(l: Any) = when(l) {
    is <!CANNOT_CHECK_FOR_ERASED!>List<String><!> -> 1
    else <!SYNTAX!>2<!>
}
