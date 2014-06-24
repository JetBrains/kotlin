import java.util.*;

interface wildcardExtendsTypeParameter {
    interface SuperWildcard<T, U> {
        Map<? extends T, ? extends U> typeForSubstitute();
    }

    interface MidWildcard<E, K> extends SuperWildcard<E, K> {
    }
}