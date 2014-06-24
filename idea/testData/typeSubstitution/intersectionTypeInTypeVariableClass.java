import java.util.*;

interface intersectionTypeInTypeVariableClass {
    interface Super<T> {
        <R extends Class<R> & List<T>> TypeVariable<R> typeForSubstitute();
    }

    interface Sub<U> extends Super<U> {
    }
}