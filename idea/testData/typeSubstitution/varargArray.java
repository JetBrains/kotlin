import java.util.*;

interface varargArray {
    interface Super<T> {
        void typeForSubstitute(T... a);
    }

    interface Sub<Integer> extends Super<Integer[]> {
    }
}