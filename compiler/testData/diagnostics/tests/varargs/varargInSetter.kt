// RUN_PIPELINE_TILL: FRONTEND
class My {
    var x: String = ""
        set(<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> value) {
            x = value
        }
}