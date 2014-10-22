trait ExtMap<K, V> : Map<K, V>
class HashMapEx<K, V> : java.util.HashMap<K, V>(), ExtMap<K, V>