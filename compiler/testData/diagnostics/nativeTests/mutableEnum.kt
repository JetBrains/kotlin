// FILE: annotation.kt

package kotlin.native.concurrent

// FILE: test.kt

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

var global = 0

enum class ImmutableEnum(val immutable: Int) {
    ONE(1), TWO(2);
}

enum class MutableEnum(<!MUTABLE_ENUM!>var mutable: Int<!>) {
    ONE(1), TWO(2);
}

enum class EnumWithSetter {
    ONE;
    var fieldWithSetter = 0
        set(value) {
            global = value
        }
}

enum class EnumWithSetterField {
    ONE;
    var fieldWithSetter = 0
        set(value) {
            field = value
        }
}

enum class EnumWithMutableFieldImplementedByDelegate {
    ONE;
    var delegatedField: Int by Delegate()
}

enum class ImmutableEnumEntry {
    ONE {
        val immutable = 2
    }
}

enum class MutableEnumEntry {
    ONE {
        <!MUTABLE_ENUM!>var mutable = 2<!>
    }
}

enum class EnumEntryWithSetter {
    ONE {
        var fieldWithSetter = 0
            set(value) {
                global = value
            }
    }
}

enum class EnumEntryWithMutableFieldImplementedByDelegate {
    ONE {
        var delegatedField: Int by Delegate()
    }
}
