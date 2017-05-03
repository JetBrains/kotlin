public class UsingReadOnlyInterfaces {
    public static class Collections {
        public static <E> void useICollection(ICollection<E> iCollection, E elem, java.util.Collection<E> other) {
            java.util.Iterator<E> iter = iCollection.iterator();
            iCollection.addAll(other);
            iCollection.add(elem);
            iCollection.isEmpty();
            iCollection.clear();
            iCollection.<error>getSize</error>(); // this is not an error when analyzing against kotlin sources (which is a bug), this inconsistency is hard to fix with the current approach
            iCollection.size();
        }

        public static <E> void useCCollection(CCollection<E> cCollection, E elem, java.util.Collection<E> other) {
            java.util.Iterator<E> iter = cCollection.iterator();
            cCollection.addAll(other);
            cCollection.add(elem);
            cCollection.isEmpty();
            cCollection.clear();
            cCollection.getSize();
            cCollection.size();
            cCollection.contains(elem);
            cCollection.contains("sasd");
            cCollection.removeAll(other);
            cCollection.retainAll(other);
        }

        public static class ExtendCCollection extends CCollection<String> {
            @Override
            public void clear() {}
        }
    }

    public static class Maps {
        public static <E, V> void useCMap(CMap<E, V> cMap) {
            cMap.isEmpty();
            java.util.Set<E> s = cMap.getKeys();
            s = cMap.keySet();
            java.util.Collection<V> v = cMap.getValues();
            v = cMap.values();
            java.util.Set<java.util.Map.Entry<E, V>> e = cMap.entrySet();
            e = cMap.getEntries();
        }
    }
}