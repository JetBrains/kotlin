fun test() {
    pcla { otvOwner ->
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.fix()
        otvOwner.constrain(InferenceTarget)
    }
    pcla { otvOwner ->
        otvOwner.constrain(materialize())
        <!BUILDER_INFERENCE_STUB_RECEIVER!>otvOwner.provide()<!>.fix()
        otvOwner.constrain(InferenceTarget)
    }
    pcla { otvOwner ->
        otvOwner.constrain(InferenceTarget)
        otvOwner.provide().<!UNRESOLVED_REFERENCE!>unresolved<!>
    }
}


class TypeVariableOwner<T> {
    fun constrain(subtypeValue: T) {}
    fun provide(): T = null!!
}

fun <OT> pcla(lambda: (TypeVariableOwner<OT>) -> Unit): OT = null!!

fun <PH> PH.fix() {}

fun <MT> materialize(): MT = null!!

object InferenceTarget
