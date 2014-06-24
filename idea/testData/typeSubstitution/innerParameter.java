import java.util.*;

interface innerParameter {
    interface SuperInnerParam<T> {
        <T> T typeForSubstitute();
    }

    interface MidInnerParam<U> extends SuperInnerParam<U> {
    }
}