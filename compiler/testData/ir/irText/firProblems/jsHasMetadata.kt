import kotlin.reflect.KProperty

// WITH_REFLECT
// WITH_RUNTIME
// FULL_JDK

// FILE: JsName.java

/**
 * An abstract base class for named JavaScript objects.
 */
public class JsName extends HasMetadata {
    @NotNull
    private final String ident;

    private final boolean temporary;

    /**
     * @param ident the unmangled ident to use for this name
     */
    public JsName(@NotNull String ident, boolean temporary) {
        this.ident = ident;
        this.temporary = temporary;
    }

    public JsName(@NotNull String ident) {
        this(ident, false);
    }

    public boolean isTemporary() {
        return temporary;
    }

    @NotNull
    public String getIdent() {
        return ident;
    }

    @Override
    public String toString() {
        return ident;
    }
}


// FILE: HasMetadata.kt

abstract class HasMetadata {
    private val metadata: MutableMap<String, Any?> = hashMapOf()

    fun <T> getData(key: String): T {
        @Suppress("UNCHECKED_CAST")
        return metadata[key] as T
    }

    fun <T> setData(key: String, value: T) {
        metadata[key] = value
    }

    fun hasData(key: String): Boolean {
        return metadata.containsKey(key)
    }

    fun removeData(key: String) {
        metadata.remove(key)
    }

    fun copyMetadataFrom(other: HasMetadata) {
        metadata.putAll(other.metadata)
    }
}

// FILE: MetadataProperty.kt

class MetadataProperty<in T : HasMetadata, R>(val default: R) {
    operator fun getValue(thisRef: T, desc: KProperty<*>): R {
        if (!thisRef.hasData(desc.name)) return default
        return thisRef.getData<R>(desc.name)
    }

    operator fun setValue(thisRef: T, desc: KProperty<*>, value: R) {
        if (value == default) {
            thisRef.removeData(desc.name)
        }
        else {
            thisRef.setData(desc.name, value)
        }
    }
}

