
import java.util.List;

fun ff(a: Any) = <!UNCHECKED_CAST!>a as List<String><!>
