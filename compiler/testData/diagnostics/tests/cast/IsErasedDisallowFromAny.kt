
import java.util.List;

fun ff(l: Any) = l is <!CANNOT_CHECK_FOR_ERASED!>List<String><!>
