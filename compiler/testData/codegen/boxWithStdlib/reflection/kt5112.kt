package test

class G<T>(val s: T) {

}

public trait ErrorsJvmTrait {
    class object {
        public val param : G<String> = G("STRING")
    }
}

public class ErrorsJvmClass {
    class object {
        public val param : G<String> = G("STRING")
    }
}

fun box(): String {
    val genericType = javaClass<ErrorsJvmTrait>().getField("param").getGenericType()
    if (genericType.toString() != "test.G<java.lang.String>") return "fail1: $genericType"

    val genericType2 = javaClass<ErrorsJvmClass>().getField("param").getGenericType()
    if (genericType2.toString() != "test.G<java.lang.String>") return "fail1: genericType2"
    return "OK"
}