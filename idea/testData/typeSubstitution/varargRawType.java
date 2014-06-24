import java.util.*;

interface varargRawType {
    interface Super<T> {
        void typeForSubstitute(T... a);
    }

    interface Sub extends Super {
    }
}