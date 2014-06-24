import java.util.*;

interface twoParametersInSubClass {
    interface SuperTwoParams<T> {
        List<T> typeForSubstitute();
    }

    interface MidTwoParams<T, S> extends SuperTwoParams<S> {
    }
}