import java.util.*;

class WeirdComparator<T> {
    public T max(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }
}
