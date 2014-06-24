import java.util.*;

interface rawExtendsWildcard {
    interface SuperRawWild<T> {
        List<? extends T> typeForSubstitute();
    }

    interface MidRawWildcard extends SuperRawWild {
    }
}