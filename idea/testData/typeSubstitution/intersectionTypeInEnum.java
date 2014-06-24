import java.util.*;

interface intersectionTypeInEnum {
    interface Super<T> {
        <R extends Enum<R> & List<T>> Enum<R> typeForSubstitute();
    }

    interface Sub<U> extends Super<U> {
    }
}