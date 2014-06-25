import java.util.*;

interface unboundedWildcardToTypeParameter {
    interface SupList<K> extends List<K> {
        @Override
        boolean retainAll(Collection<K> c); // error, check that we do not fall
    }
}