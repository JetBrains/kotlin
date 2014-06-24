import java.util.*;

interface twoParameters {
    interface SuperTwoParams<T, U> {
        Map<T, List<U>> typeForSubstitute();
    }

    interface MidTwoParams<E> extends SuperTwoParams<Integer, E> {
    }
}