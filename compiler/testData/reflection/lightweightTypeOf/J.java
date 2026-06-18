import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;

public class J {
    public static class JavaBox<T> {}

    public static class JavaPair<T, U> {}

    public static class JavaFunctionBoundBox<T extends kotlin.jvm.functions.Function0<String>> {}

    public static class JavaInterfaceBoundBox<T extends JavaInterface<String>> {}

    public interface JavaInterface<T> {
        T value();
    }

    public static class JavaBase<T> {
        public T value() { return null; }
    }

    public interface JavaInterfaceExtendsKotlinFunction extends kotlin.jvm.functions.Function0<String> {}

    public static class JavaClassImplementsKotlinFunction implements kotlin.jvm.functions.Function1<String, Integer> {
        @Override
        public Integer invoke(String value) { return null; }
    }

    public static String nullabilityFlexible() { return null; }

    @NotNull
    public static List<String> mutabilityFlexible() { return null; }

    public static List<String> bothFlexible() { return null; }

    public static int[] primitiveArray() { return null; }

    public static String[] stringArray() { return null; }

    @NotNull
    public static String[] nonNullStringArray() { return null; }

    public static List<String[]> listOfStringArrays() { return null; }

    public static List<? extends Number> wildcardOut() { return null; }

    public static List<? super Integer> wildcardIn() { return null; }

    public static List<?> unboundedWildcard() { return null; }

    public static Map<String, List<Integer[]>> nestedGeneric() { return null; }

    public static JavaBox<String> boxOfString() { return null; }

    public static JavaBox<? extends Number> boxOfWildcard() { return null; }

    public static List rawList() { return null; }

    public static JavaBox rawBox() { return null; }

    public static JavaInterfaceExtendsKotlinFunction javaInterfaceExtendsKotlinFunction() { return null; }

    public static JavaClassImplementsKotlinFunction javaClassImplementsKotlinFunction() { return null; }

    public static JavaBox<JavaClassImplementsKotlinFunction> boxOfJavaClassImplementsKotlinFunction() { return null; }
}
