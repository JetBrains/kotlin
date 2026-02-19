// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

open class VisibilityCasesBase {
    protected open val vProtectedOpenProp: String = ""
    internal open val vInternalOpenProp: Double = 1.0
    open val vPublicOpenProp: Boolean = true
}

open class VisibilityCasesChild : VisibilityCasesBase() {
    override val vProtectedOpenProp: String = ""
    override val vInternalOpenProp: Double = 1.0
    override val vPublicOpenProp: Boolean = true
}

val visibilityChild = VisibilityCasesChild()

val VisibilityCasesChild.extPublicVal: Int get() = 1
internal val VisibilityCasesChild.extInternalVal: Int get() = 2

class GetterSetterHost(initial: Int = 1) {
    var gsPublicPrivateSet: Int = initial
        private set
    private val gsPrivateVal: Int = initial
}
val gsHost = GetterSetterHost()

open class ProtectedPropConsumer : VisibilityCasesChild() {
    fun destructureProtectedInside() {
        val (p = vProtectedOpenProp) = this
        val (ok = vPublicOpenProp) = this
        p.length
        ok.not()
    }
}

fun VisibilityCasesChild.destructureProtectedFromExtensionNegative() {
    val (p = <!INVISIBLE_REFERENCE!>vProtectedOpenProp<!>) = this
}

fun internalOnBaseRefPositive(b: VisibilityCasesBase) {
    val (d = vInternalOpenProp) = b
    d.compareTo(1)
}

fun internalOnChildRefPositive(c: VisibilityCasesChild) {
    val (d = vInternalOpenProp) = c
    d.compareTo(1)
}

fun extPropsSameModulePositive() {
    val (d = extPublicVal) = visibilityChild
    d.compareTo(1)
}

fun extInternalPropsSameModulePositive() {
    val (x = extInternalVal) = visibilityChild
    x.compareTo(1)
}

fun backtickedRenamePositive() {
    val (`is-public` = vPublicOpenProp, `internal#` = vInternalOpenProp) = visibilityChild
    `is-public`.not()
    `internal#`.compareTo(1)
}

fun backtickedRenameNegative() {
    val (`prot` = <!INVISIBLE_REFERENCE!>vProtectedOpenProp<!>) = visibilityChild
}

val VisibilityCasesChild.<!EXTENSION_SHADOWED_BY_MEMBER!>vPublicOpenProp<!>: Int get() = 42

fun memberVsExtensionNameClashPositive() {
    val (ok = vPublicOpenProp) = visibilityChild
    ok.not()
}

fun getterSetterVisibilityPositive() {
    val (x = gsPublicPrivateSet) = gsHost
    x.compareTo(0)
}

fun getterSetterVisibilityNegative() {
    val (bad = <!INVISIBLE_REFERENCE!>gsPrivateVal<!>) = gsHost
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, localProperty, override, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver,
stringLiteral, thisExpression */
