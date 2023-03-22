class OpenClassImpl : OpenClass() {
    override var openNonInlineToInlineProperty: String
        get() = "OpenClassImpl.openNonInlineToInlineProperty"
        set(value) { lastRecordedState = "OpenClassImpl.openNonInlineToInlineProperty=$value" }

    override var openNonInlineToInlinePropertyWithDelegation: String
        get() = super.openNonInlineToInlinePropertyWithDelegation + " called from OpenClassImpl.openNonInlineToInlinePropertyWithDelegation"
        set(value) { super.openNonInlineToInlinePropertyWithDelegation = "$value called from OpenClassImpl.openNonInlineToInlinePropertyWithDelegation" }

    var newInlineProperty1: String // overrides accidentally appeared inline property
        get() = "OpenClassImpl.newInlineProperty1"
        set(value) { lastRecordedState = "OpenClassImpl.newInlineProperty1=$value" }

    inline var newInlineProperty2: String // overrides accidentally appeared inline property
        get() = "OpenClassImpl.newInlineProperty2"
        set(value) { lastRecordedState = "OpenClassImpl.newInlineProperty2=$value" }

    inline var newNonInlineProperty: String // overrides accidentally appeared non-inline function
        get() = "OpenClassImpl.newNonInlineProperty"
        set(value) { lastRecordedState = "OpenClassImpl.newNonInlineProperty=$value" }
}

fun openNonInlineToInlinePropertyInOpenClass(oc: OpenClass): String = oc.openNonInlineToInlineProperty
fun openNonInlineToInlinePropertyWithDelegationInOpenClass(oc: OpenClass): String = oc.openNonInlineToInlinePropertyWithDelegation
fun newInlineProperty1InOpenClass(oc: OpenClass): String = oc.newInlineProperty1Reader()
fun newInlineProperty2InOpenClass(oc: OpenClass): String = oc.newInlineProperty2Reader()
fun newNonInlinePropertyInOpenClass(oc: OpenClass): String = oc.newNonInlinePropertyReader()

fun openNonInlineToInlinePropertyInOpenClassImpl(oci: OpenClassImpl): String = oci.openNonInlineToInlineProperty
fun openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(oci: OpenClassImpl): String = oci.openNonInlineToInlinePropertyWithDelegation
fun newInlineProperty1InOpenClassImpl(oci: OpenClassImpl): String = oci.newInlineProperty1
fun newInlineProperty2InOpenClassImpl(oci: OpenClassImpl): String = oci.newInlineProperty2
fun newNonInlinePropertyInOpenClassImpl(oci: OpenClassImpl): String = oci.newNonInlineProperty

fun openNonInlineToInlinePropertyInOpenClass(oc: OpenClass, value: String): String { oc.openNonInlineToInlineProperty = value; return oc.lastRecordedState }
fun openNonInlineToInlinePropertyWithDelegationInOpenClass(oc: OpenClass, value: String): String { oc.openNonInlineToInlinePropertyWithDelegation = value; return oc.lastRecordedState }
fun newInlineProperty1InOpenClass(oc: OpenClass, value: String): String { oc.newInlineProperty1Writer(value); return oc.lastRecordedState }
fun newInlineProperty2InOpenClass(oc: OpenClass, value: String): String { oc.newInlineProperty2Writer(value); return oc.lastRecordedState }
fun newNonInlinePropertyInOpenClass(oc: OpenClass, value: String): String { oc.newNonInlinePropertyWriter(value); return oc.lastRecordedState }

fun openNonInlineToInlinePropertyInOpenClassImpl(oci: OpenClassImpl, value: String): String { oci.openNonInlineToInlineProperty = value; return oci.lastRecordedState }
fun openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(oci: OpenClassImpl, value: String): String { oci.openNonInlineToInlinePropertyWithDelegation = value; return oci.lastRecordedState }
fun newInlineProperty1InOpenClassImpl(oci: OpenClassImpl, value: String): String { oci.newInlineProperty1 = value; return oci.lastRecordedState }
fun newInlineProperty2InOpenClassImpl(oci: OpenClassImpl, value: String): String { oci.newInlineProperty2 = value; return oci.lastRecordedState }
fun newNonInlinePropertyInOpenClassImpl(oci: OpenClassImpl, value: String): String { oci.newNonInlineProperty = value; return oci.lastRecordedState }
