import java.util.*;

public class max {
    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
        return Collections.max(coll);
    }
}
