import java.util.*;

interface intersectionTypeAsTypeParameter {
    interface Super<K> {
        <T extends Enum<T> & List<K>> Map<? extends T, K> typeForSubstitute();
    }

    interface Sub<U> extends Super<U> {
    }
}