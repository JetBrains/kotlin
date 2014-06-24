import java.util.*;

interface wildcardExtends {
    interface SuperWildcardExtends<T> {
        List<? extends T> typeForSubstitute();
    }

    interface MidWildcardExtends extends SuperWildcardExtends<Integer> {
    }
}