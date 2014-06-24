import java.util.*;

interface unboundedWildcard {
    interface Super<T> {
        List<?> typeForSubstitute();
    }

    interface Sub extends Super<Integer> {
    }
}