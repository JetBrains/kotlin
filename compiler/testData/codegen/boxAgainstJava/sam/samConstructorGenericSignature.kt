import java.util.Arrays

fun box(): String {
    val r: JavaClass.Computable<String> = JavaClass.Computable { "OK" }
    val supertypes = Arrays.toString(r.javaClass.getGenericInterfaces())
    if (supertypes != "[JavaClass.JavaClass\$Computable<java.lang.String>]") return "Fail: $supertypes"
    return JavaClass.compute(r)!!
}
