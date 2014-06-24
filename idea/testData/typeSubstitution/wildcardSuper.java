import java.util.*;

interface wildcardSuper {
    interface SuperWildcardSuper<T> {
        List<? extends T> typeForSubstitute();
    }

    interface MidWildcardSuper extends SuperWildcardSuper<Integer> {
    }
}