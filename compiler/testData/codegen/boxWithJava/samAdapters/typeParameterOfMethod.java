import java.util.*;

class WeirdComparator {
    public static <T> T max(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }

    public static <T extends CharSequence> T max2(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }
}
