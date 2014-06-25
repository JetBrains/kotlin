import java.util.*;

interface varargToClassWithWildcard {
    interface Super<T> {
        void typeForSubstitute(T... a);
    }

    interface Sub<E> extends Super<List<? extends E>> {
    }
}