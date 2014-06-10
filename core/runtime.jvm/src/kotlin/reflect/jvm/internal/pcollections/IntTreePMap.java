package kotlin.reflect.jvm.internal.pcollections;

/**
 * An efficient persistent map from integer keys to non-null values.
 * <p/>
 * Iteration occurs in the integer order of the keys.
 * <p/>
 * This implementation is thread-safe, although its iterators may not be.
 * <p/>
 * The balanced tree is based on the Glasgow Haskell Compiler's Data.Map implementation,
 * which in turn is based on "size balanced binary trees" as described by:
 * <p/>
 * Stephen Adams, "Efficient sets: a balancing act",
 * Journal of Functional Programming 3(4):553-562, October 1993,
 * http://www.swiss.ai.mit.edu/~adams/BB/.
 * <p/>
 * J. Nievergelt and E.M. Reingold, "Binary search trees of bounded balance",
 * SIAM journal of computing 2(1), March 1973.
 *
 * @author harold
 */
final class IntTreePMap<V> {
    private static final IntTreePMap<Object> EMPTY = new IntTreePMap<Object>(IntTree.EMPTYNODE);

    @SuppressWarnings("unchecked")
    public static <V> IntTreePMap<V> empty() {
        return (IntTreePMap<V>) EMPTY;
    }

    private final IntTree<V> root;

    private IntTreePMap(IntTree<V> root) {
        this.root = root;
    }

    private IntTreePMap<V> withRoot(IntTree<V> root) {
        if (root == this.root) return this;
        return new IntTreePMap<V>(root);
    }

    public V get(int key) {
        return root.get(key);
    }

    public IntTreePMap<V> plus(int key, V value) {
        return withRoot(root.plus(key, value));
    }

    public IntTreePMap<V> minus(int key) {
        return withRoot(root.minus(key));
    }
}
