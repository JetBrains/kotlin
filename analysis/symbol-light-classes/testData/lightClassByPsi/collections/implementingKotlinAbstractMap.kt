// WITH_STDLIB
// FULL_JDK
package test

abstract class Awd : AbstractMap<String, String>() {
    override fun containsKey(key: String): Boolean {
        return super.containsKey(key)
    }

    override fun containsValue(value: String): Boolean {
        return super.containsValue(value)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun get(key: String): String? {
        return super.get(key)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun isEmpty(): Boolean {
        return super.isEmpty()
    }

    override fun toString(): String {
        return super.toString()
    }

    override val keys: Set<String>
        get() = super.keys
    override val size: Int
        get() = super.size
    override val values: Collection<String>
        get() = super.values
}

// LIGHT_ELEMENTS_NO_DECLARATION: Awd.class[getOrDefault;getOrDefault]
