// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
data class Data private constructor(val x: Int) {
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

fun topLevel(data: Data) {
    data.copy()
}

fun Data.topLevelExtension() {
    copy()
}

fun local() {
    data class Local private constructor(val x: Int)

    fun Local.foo() {
        copy()
    }
}

data class GenericData<A, B: CharSequence> private constructor(val a: A, val b: B) {
    fun copy(a: B, b: A) {}
    fun member() {
        copy()
        this.copy()
    }
}

fun topLevel(data: GenericData<Int, String>) {
    data.copy()
    data.copy("", 1) // fake copy
}

data class GenericDataForRef<A> private constructor(val a: A) {
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
    data.copy()
    data::copy
    GenericDataForRef<Int>::copy
    GenericDataForRef<*>::copy
    GenericDataForRef<in Int>::copy
    GenericDataForRef<out Int>::copy
}
