// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
data class Data <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>private<!> constructor(val x: Int) {
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
    data.<!DATA_CLASS_INVISIBLE_COPY_USAGE_ERROR!>copy<!>()
}

fun Data.topLevelExtension() {
    <!DATA_CLASS_INVISIBLE_COPY_USAGE_ERROR!>copy<!>()
}

fun local() {
    data class Local <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>private<!> constructor(val x: Int)

    fun Local.foo() {
        <!DATA_CLASS_INVISIBLE_COPY_USAGE_ERROR!>copy<!>()
    }
}
