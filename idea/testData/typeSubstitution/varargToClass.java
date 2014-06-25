import java.util.*;

interface varargToClass {
    interface Super<T> {
        void typeForSubstitute(T... a);
    }

    interface Sub extends Super<Integer> {
    }
}