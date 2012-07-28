
import java.util.List

fun f(a : List<out Any>) = a is <!CANNOT_CHECK_FOR_ERASED!>List<out Int><!>
