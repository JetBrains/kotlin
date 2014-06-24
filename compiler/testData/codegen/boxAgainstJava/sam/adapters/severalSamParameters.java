import java.util.*;

class JavaClass {
    public static String findMaxAndInvokeCallback(Comparator<String> comparator, String a, String b, Runnable afterRunnable) {
        int compare = comparator.compare(a, b);
        afterRunnable.run();
        return compare > 0 ? a : b;
    }
}
