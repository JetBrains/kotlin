// FULL_JDK
// LIBRARY_PLATFORMS: JVM
package test

abstract class Awd : java.util.AbstractMap<String, String>() {
    override val keys: MutableSet<String>
        get() = super.keys
}

abstract class SizedCollection : java.util.AbstractCollection<String>() {
    override val size: Int
        get() = 1
}

abstract class PlainJavaAbstractList : java.util.AbstractList<String>()

// LIGHT_ELEMENTS_NO_DECLARATION: Awd.class[containsKey;containsKey;containsValue;containsValue;entrySet;get;get;getEntries;getOrDefault;getOrDefault;getSize;getValues;keySet;remove;remove;remove;remove;size;values], PlainJavaAbstractList.class[contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;remove;remove;removeAt;size], SizedCollection.class[contains;contains;remove;remove;size]
