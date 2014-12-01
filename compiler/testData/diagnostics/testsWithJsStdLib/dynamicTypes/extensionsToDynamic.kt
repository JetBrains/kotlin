// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    d.onDynamic()
    d.onNullableDynamic()

    d.valOnDynamic
    <!VAL_REASSIGNMENT!>d.valOnDynamic<!> = 1

    d.varOnDynamic
    d.varOnDynamic = 1
}

fun dynamic.extTest() {
    onDynamic()
    onNullableDynamic()

    valOnDynamic
    <!VAL_REASSIGNMENT!>valOnDynamic<!> = 1

    varOnDynamic
    varOnDynamic = 1

    this.onDynamic()
    this.onNullableDynamic()

    this.valOnDynamic
    this.valOnDynamic = 1

    this.varOnDynamic
    this.varOnDynamic = 1

}

fun dynamic.onDynamic() {}
fun dynamic<!REDUNDANT_NULLABLE!>?<!>.onNullableDynamic() {}

val dynamic.valOnDynamic: Int get() = 1

var dynamic.varOnDynamic: Int
    get() = 1
    set(v) {}


class ForMemberExtensions {
    fun test(d: dynamic) {
        d.memberExtensionVar
        d.memberExtensionVar = 1

        d.memberExtensionVal
        <!VAL_REASSIGNMENT!>d.memberExtensionVal<!> = 1
    }

    val dynamic.memberExtensionVal: Int get() = 1
    var dynamic.memberExtensionVar: Int
        get() = 1
        set(v) {}
}