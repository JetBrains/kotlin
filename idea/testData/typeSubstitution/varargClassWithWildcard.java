import java.util.*;

interface varargClassWithWildcard {
    interface Super<T> {
        void typeForSubstitute(List<? extends T>... a);
    }

    interface Sub<E> extends Super<List<? extends E>> {
    }
}