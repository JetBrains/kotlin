package kt32691

fun main() {
    //Breakpoint!
    Unit
}

class L<R, O>

// EXPRESSION: {}.toString()
// RESULT: "kotlin.jvm.functions.Function0<kotlin.Unit>": Ljava/lang/String;

// EXPRESSION: {i: Int -> }.toString()
// RESULT: "kotlin.jvm.functions.Function1<java.lang.Integer, kotlin.Unit>": Ljava/lang/String;

// EXPRESSION: {i: Int, d: Double -> "239" }.toString()
// RESULT: "kotlin.jvm.functions.Function2<java.lang.Integer, java.lang.Double, java.lang.String>": Ljava/lang/String;

// EXPRESSION: {m : L<in String, out Int> -> ""}.toString()
// RESULT: "kotlin.jvm.functions.Function1<kt32691.L<in java.lang.String, out java.lang.Integer>, java.lang.String>": Ljava/lang/String;




