// IGNORE_BACKEND: JVM_IR
// Missing IMPLICIT_NOTNULL casts
import java.util.ArrayList

fun foo(): Any {
    val a = ArrayList<String>()
    return a.get(0)
}

fun bar(a: ArrayList<String>) {
}

// 1 checkExpressionValueIsNotNull
// 0 checkNotNullExpressionValue
// 1 checkParameterIsNotNull
// 0 checkNotNullParameter
