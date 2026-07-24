// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, +DataClassCopyRespectsConstructorVisibility
@JsExport
data class Data1 @JsExport.Ignore constructor(val x: Int) {
    fun member() {
        copy()
        this.copy()
    }

    companion object {
        fun of(): Data1 {
            return Data1(1).copy()
        }
    }
}

class NotExportedClass

@JsExport
data class Data2 @JsExport.Ignore constructor(
    <!NON_EXPORTABLE_TYPE!>val x: NotExportedClass<!>
) {
    fun member() {
        copy()
        this.copy()
    }

    companion object {
        fun of(): Data2 {
            return Data2(NotExportedClass()).copy()
        }
    }
}

@JsExport
data class Data3 @JsExport.Ignore constructor(
    @JsExport.Ignore val x: NotExportedClass
) {
    fun member() {
        copy()
        this.copy()
    }

    companion object {
        fun of(): Data3 {
            return Data3(NotExportedClass()).copy()
        }
    }
}
