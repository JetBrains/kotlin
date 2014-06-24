import java.util.Map;

interface mapEntry {
    interface Super<T, E> {
        Map.Entry<T, E> typeForSubstitute();
    }

    interface Mid<E> extends Super<E, Integer> {
    }
}