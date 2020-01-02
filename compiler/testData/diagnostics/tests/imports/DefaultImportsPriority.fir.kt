import java.lang.reflect.*
import java.util.List

fun foo(
        p1: Array<String> /* should be resolved to kotlin.Array */,
        p2: List<String> /* should be resolved to java.util.List */) { }