import java.util.*;

interface classWithWildcard {
    interface SuperList<T> {
        List<T> typeForSubstitute();
    }

    interface MidList<U> extends SuperList<List<? extends U>> {
    }
}