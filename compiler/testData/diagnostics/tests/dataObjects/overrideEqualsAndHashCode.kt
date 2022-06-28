// FIR_IDENTICAL
// LANGUAGE: +DataObjects

data object Override {
    <!DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE!>override<!> fun equals(other: Any?): Boolean {
        return true
    }

    <!DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE!>override<!> fun hashCode(): Int {
        return 1
    }
}

open class Base {
    open fun hashCode(x: Int) = x
}

data object NoOverride: Base() {
    fun equals(other: Any?, tag: Int): Boolean {
        return true
    }

    fun hashCode(param: String): Int {
        return 1
    }

    override fun hashCode(x: Int) = x + 1
}

open class Super {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return 1
    }
}

data object OverridenInSuper: Super() {
    <!DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE!>override<!> fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}
