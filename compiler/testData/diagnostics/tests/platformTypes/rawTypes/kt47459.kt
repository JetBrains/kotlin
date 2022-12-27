// FIR_IDENTICAL
// FILE: MapObjectManager.java
abstract class MapObjectManager<C extends MapObjectManager.Collection> {
    public class Collection {

    }
    public C getCollection(String id) {
        return null;
    }
}

// FILE: MarkerManager.java
public class MarkerManager extends MapObjectManager<MarkerManager.Collection> {
    public class Collection extends MapObjectManager.Collection {
        public void setOnMarkerClickListener() {
        }
    }
}

// FILE: main.kt
fun foo(markerManager: MarkerManager) {
    val test: MarkerManager.Collection = markerManager.getCollection("FOO")!!
    test.setOnMarkerClickListener()
}