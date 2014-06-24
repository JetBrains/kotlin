import java.util.*;

interface varargClass {
    interface Super<T> {
        void typeForSubstitute(List<T>... a);
    }

    interface Sub extends Super<Integer> {
    }
}