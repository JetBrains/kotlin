
import java.util.List;
import java.util.Collection;

fun ff(c: Collection<String>) = c <!CAST_NEVER_SUCCEEDS!>as<!> List<Int>
