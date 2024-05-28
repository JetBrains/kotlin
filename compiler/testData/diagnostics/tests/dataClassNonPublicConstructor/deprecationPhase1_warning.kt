// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
data class Data private constructor(val x: Int) {
    fun member() {
        copy()
        this.copy()
        ::copy
        this::copy
        Data::copy
    }

    companion object {
        fun of(): Data {
            return Data(1).copy()
        }
    }
}

fun topLevel(data: Data) {
    data.copy()
    data::copy
    Data::copy
}

fun Data.topLevelExtension() {
    copy()
    ::copy
    Data::copy
}

fun local() {
    data class Local private constructor(val x: Int)

    fun Local.foo() {
        copy()
        ::copy
        Local::copy
    }
}

data class GenericData<T> private constructor(val value: T) {
    fun member() {
        copy()
        this.copy()
        ::copy
        this::copy
        GenericData<Int>::copy
        GenericData<*>::copy
        GenericData<in Int>::copy
        GenericData<out Int>::copy
    }
}

fun topLevel(data: GenericData<Int>) {
    data.copy()
    data::copy
    GenericData<Int>::copy
    GenericData<*>::copy
    GenericData<in Int>::copy
    GenericData<out Int>::copy
}
