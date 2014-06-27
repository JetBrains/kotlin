import java.util.Arrays;
import java.util.Comparator;

class JavaClass {
    public static String foo(Comparator<String> comparator) {
        return Arrays.toString(comparator.getClass().getGenericInterfaces());
    }
}
