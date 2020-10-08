// IGNORE_BACKEND_FIR: JVM_IR
import java.util.ArrayList

fun foo(): Any {
    val a = ArrayList<String>()
    return a.get(0)
}

fun bar(a: ArrayList<String>) {
}

// 0 checkExpressionValueIsNotNull
// 1 checkNotNullExpressionValue
// 0 checkParameterIsNotNull
// 1 checkNotNullParameter
