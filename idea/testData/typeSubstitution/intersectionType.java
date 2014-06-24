import java.util.*;

interface intersectionType {
    interface SuperIntersection<T> {
        <R extends Enum<R> & List<T>> R typeForSubstitute();
    }

    interface MidIntersection<U> extends SuperIntersection<U> {
    }
}