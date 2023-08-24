@file:Suppress("unused", "UNUSED_PARAMETER", "NOTHING_TO_INLINE")

open class OpenClass {
    @Suppress("MemberVisibilityCanBePrivate")
    var lastRecordedState: String = ""

    open var openNonInlineToInlineProperty: String
        get() = "OpenClass.openNonInlineToInlineProperty"
        set(value) { lastRecordedState = "OpenClass.openNonInlineToInlineProperty=$value" }

    open var openNonInlineToInlinePropertyWithDelegation: String
        get() = "OpenClass.openNonInlineToInlinePropertyWithDelegation"
        set(value) { lastRecordedState = "OpenClass.openNonInlineToInlinePropertyWithDelegation=$value" }

    //inline var newInlineProperty1: String
    //    get() = "OpenClass.newInlineProperty1"
    //    set(value) { lastRecordedState = "OpenClass.newInlineProperty1=$value" }

    //inline var newInlineProperty2: String
    //    get() = "OpenClass.newInlineProperty2"
    //    set(value) { lastRecordedState = "OpenClass.newInlineProperty2=$value" }

    //var newNonInlineProperty: String
    //    get() = "OpenClass.newNonInlineProperty"
    //    set(value) { lastRecordedState = "OpenClass.newNonInlineProperty=$value" }

    fun newInlineProperty1Reader(): String = TODO("Not implemented: OpenClass.newInlineProperty1Reader()")
    fun newInlineProperty2Reader(): String = TODO("Not implemented: OpenClass.newInlineProperty2Reader()")
    fun newNonInlinePropertyReader(): String = TODO("Not implemented: OpenClass.newNonInlinePropertyReader()")

    fun newInlineProperty1Writer(value: String): Unit = TODO("Not implemented: OpenClass.newInlineProperty1Writer()")
    fun newInlineProperty2Writer(value: String): Unit = TODO("Not implemented: OpenClass.newInlineProperty2Writer()")
    fun newNonInlinePropertyWriter(value: String): Unit = TODO("Not implemented: OpenClass.newNonInlinePropertyWriter()")
}
