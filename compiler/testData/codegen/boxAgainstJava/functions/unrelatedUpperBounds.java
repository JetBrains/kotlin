import java.util.Collection;

public class unrelatedUpperBounds {
    public static <T extends CharSequence & java.io.Serializable> T id(T p) {
        return p;
    }
}
