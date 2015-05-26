import kotlin.jvm.functions.Function2;
import kotlin.reflect.KMemberFunction;

public class J {
    public static String go() {
        KMemberFunction<K, String> fun = K.Companion.getRef();
        Object result = ((Function2) fun).invoke(new K(), "KO");
        return (String) result;
    }
}
