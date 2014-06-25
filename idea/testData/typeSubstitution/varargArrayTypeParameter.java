import java.util.*;

interface varargArrayTypeParameter {
    interface Super<T> {
        void typeForSubstitute(T... a);
    }

    interface Sub<E> extends Super<E[]> {
    }
}