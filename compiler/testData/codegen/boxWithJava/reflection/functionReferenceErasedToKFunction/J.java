import kotlin.jvm.functions.Function2;
import kotlin.reflect.KFunction;

public class J {
    public static String go() {
        KFunction<String> fun = K.Companion.getRef();
        Object result = ((Function2) fun).invoke(new K(), "KO");
        return (String) result;
    }
}
