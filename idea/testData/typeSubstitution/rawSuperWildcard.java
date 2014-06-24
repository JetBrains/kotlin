import java.util.*;

interface rawSuperWildcard {
    interface SuperRawWild<T> {
        List<? super T> typeForSubstitute();
    }

    interface MidRawWildcard extends SuperRawWild {
    }
}