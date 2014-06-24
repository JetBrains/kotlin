import java.util.*;

interface rawWildcardWithBound {
    interface Super<T extends Cloneable> {
        List<? extends T> typeForSubstitute();
    }

    interface Sub extends Super {
    }
}