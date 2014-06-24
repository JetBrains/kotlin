import java.util.*;

interface wildcardToWildcard {
    interface SupList<K> extends List<K> {
        @Override
        boolean addAll(Collection<? extends K> c);
    }
}