package test

class G<T>(val s: T) {

}

public interface ErrorsJvmTrait {
    companion object {
        public val param : G<String> = G("STRING")
    }
}

public class ErrorsJvmClass {
    companion object {
        public val param : G<String> = G("STRING")
    }
}

fun box(): String {
    val genericTypeInTrait = javaClass<ErrorsJvmTrait>().getField("param").getGenericType()
    if (genericTypeInTrait.toString() != "test.G<java.lang.String>") return "fail1: $genericTypeInTrait"

    val genericTypeInClassObject = ErrorsJvmTrait.javaClass.getDeclaredField("param").getGenericType()
    if (genericTypeInClassObject.toString() != "test.G<java.lang.String>") return "fail1: $genericTypeInClassObject"

    val genericTypeInClass = javaClass<ErrorsJvmClass>().getField("param").getGenericType()
    if (genericTypeInClass.toString() != "test.G<java.lang.String>") return "fail1: genericTypeInClass"
    return "OK"
}