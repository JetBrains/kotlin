// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
data class Data <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val x: Int) {
    fun member() {
        copy()
        this.copy()
    }

    companion object {
        fun of(): Data {
            return Data(1).copy()
        }
    }
}

data class VarargData <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val value: IntArray) {
    fun copy(vararg value: Int): VarargData = null!!
}

data object DataObject {
    fun copy(): DataObject = null!!
}

fun topLevel(data: Data, varargData: VarargData) {
    data.<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>()
    varargData.copy(42, 42)
    DataObject.copy()
}

fun Data.topLevelExtension() {
    <!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>()
}

fun local() {
    data class Local <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val x: Int)

    fun Local.foo() {
        <!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>()
    }
}

data class GenericData<A, B: CharSequence> <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val a: A, val b: B) {
    fun copy(a: B, b: A) {}
    fun member() {
        copy()
        this.copy()
    }
}

fun topLevel(data: GenericData<Int, String>) {
    data.<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>()
    data.copy("", 1) // fake copy
}

data class GenericDataForRef<A> <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val a: A) {
    fun member() {
        copy()
        this.copy()
        ::copy
        this::copy
        GenericDataForRef<Int>::copy
        GenericDataForRef<*>::copy
        GenericDataForRef<in Int>::copy
        GenericDataForRef<out Int>::copy
    }
}

fun topLevel(data: GenericDataForRef<Int>) {
    data.<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>()
    data::<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>
    GenericDataForRef<Int>::<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>
    GenericDataForRef<*>::<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>
    GenericDataForRef<in Int>::<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>
    GenericDataForRef<out Int>::<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>
}
