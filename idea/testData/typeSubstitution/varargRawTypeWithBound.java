import java.util.*;

interface varargRawTypeWithBound {
    interface Super<T extends Integer> {
        void typeForSubstitute(T... a);
    }

    interface Sub<E> extends Super {
    }
}