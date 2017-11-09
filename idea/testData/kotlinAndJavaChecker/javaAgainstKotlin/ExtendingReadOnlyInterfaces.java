public class EtendingReadOnlyInterfaces {
    public static class Lists {
        <error>public static class ExtendIList1 implements IList<String></error> {

        }

        <error>public static class ExtendIList2<E> implements IList<E></error> {

        }

        public static class ExtendCList1<E> extends CList<E> {

        }

        public static class ExtendCList2<E> extends CList<String> {

        }

        public static class ExtendSList extends SList {

        }

        public static class ExtendAList extends AList {

        }
    }  
    
    public static class Collections {
        <error>public static class ExtendICollection1 implements ICollection<String></error> {

        }

        <error>public static class ExtendICollection2<E> implements ICollection<E></error> {

        }

        public static class ExtendCCollection1<E> extends CCollection<E> {

        }

        public static class ExtendCCollection2<E> extends CCollection<String> {

        }

        public static class ExtendSCollection extends SCollection {

        }

        public static class ExtendACollection extends ACollection {

        }
    }    
    
    public static class Sets {
        <error>public static class ExtendISet1 implements ISet<String></error> {

        }

        <error>public static class ExtendISet2<E> implements ISet<E></error> {

        }

        public static class ExtendCSet1<E> extends CSet<E> {

        }

        public static class ExtendCSet2<E> extends CSet<String> {

        }

        public static class ExtendSSet extends SSet {

        }

        public static class ExtendASet extends ASet {

        }
    }    
    
    public static class Iterables {
        <error>public static class ExtendIIterable1 implements IIterable<String></error> {

        }

        <error>public static class ExtendIIterable2<E> implements IIterable<E></error> {

        }

        public static class ExtendCIterable1<E> extends CIterable<E> {

        }

        public static class ExtendCIterable2<E> extends CIterable<String> {

        }

        public static class ExtendSIterable extends SIterable {

        }

        public static class ExtendAIterable extends AIterable {

        }
    }    
    
    public static class Iterators {
        <error>public static class ExtendIIterator1 implements IIterator<String></error> {

        }

        <error>public static class ExtendIIterator2<E> implements IIterator<E></error> {

        }

        public static class ExtendCIterator1<E> extends CIterator<E> {

        }

        public static class ExtendCIterator2<E> extends CIterator<String> {

        }

        public static class ExtendSIterator extends SIterator {

        }

        public static class ExtendAIterator extends AIterator {

        }
    }
    
    public static class Maps {
        <error>public static class ExtendIMap1 implements IMap<String, Integer></error> {

        }

        <error>public static class ExtendIMap2<E> implements IMap<String, E></error> {

        }

        public static class ExtendCMap1<K, V> extends CMap<K, V> {

        }

        public static class ExtendCMap2<V> extends CMap<String, V> {

        }

        // NOTE: looks like a bug in compiler see KT-17738

        //public static class ExtendSMap extends SMap<A> {
        //
        //}
        //
        //public static class ExtendABMap extends ABMap {
        //
        //}
    }

    public static class MapEntrys {
        <error>public static class ExtendIMapEntry1 implements IMapEntry<String, Integer></error> {

        }

        <error>public static class ExtendIMapEntry2<E> implements IMapEntry<String, E></error> {

        }

        public static class ExtendCMapEntry1<K, V> extends CMapEntry<K, V> {

        }

        public static class ExtendCMapEntry2<V> extends CMapEntry<String, V> {

        }

        public static class ExtendSMapEntry extends SMapEntry<A> {

        }

        public static class ExtendAMapEntry extends ABMapEntry {

        }
    }
}