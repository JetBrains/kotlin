// FIR_IDENTICAL

@OptIn(ExperimentalStdlibApi::class)
@JsExternalInheritorsOnly
external interface ExternalInterfaceX {
    val x: String
}

@OptIn(ExperimentalStdlibApi::class)
@JsExternalInheritorsOnly
external open class ExternalOpenClassX {
    val x: String
}

// check interfaces

external interface ExternalInterfaceXY : ExternalInterfaceX {
    val y: String
}

interface <!JS_EXTERNAL_INHERITORS_ONLY!>InterfaceXY<!> : ExternalInterfaceX {
    val y: String
}

interface <!JS_EXTERNAL_INHERITORS_ONLY!>InterfaceXYZ<!> : ExternalInterfaceXY {
    val z: String
}

// check objects

external object ExternalObjectXY : ExternalInterfaceX {
    override val x: String
    val y: String
}

external object ExternalObjectXYZ : ExternalInterfaceXY {
    override val x: String
    override val y: String
    val z: String
}

external object ExternalObjectXZ : ExternalOpenClassX {
    val z: String
}

<!JS_EXTERNAL_INHERITORS_ONLY!>object ObjectXY<!> : ExternalInterfaceX {
    override val x: String = "X"
    val y: String = "Y"
}

<!JS_EXTERNAL_INHERITORS_ONLY!>object ObjectXYZ<!> : ExternalInterfaceXY {
    override val x: String = "X"
    override val y: String = "Y"
    val z: String = "Z"
}

<!JS_EXTERNAL_INHERITORS_ONLY!>object ObjectXZ<!> : ExternalOpenClassX() {
    val z: String = "Z"
}

// check classes

external class ExternalClassXY : ExternalInterfaceX {
    override val x: String
    val y: String
}

external class ExternalClassXYZ : ExternalInterfaceXY {
    override val x: String
    override val y: String
    val z: String
}

external open class ExternalOpenClassXZ : ExternalOpenClassX {
    val z: String
}

class <!JS_EXTERNAL_INHERITORS_ONLY!>ClassXY<!> : ExternalInterfaceX {
    override val x: String = "X"
    val y: String = "Y"
}

class <!JS_EXTERNAL_INHERITORS_ONLY!>ClassXYZ<!> : ExternalInterfaceXY {
    override val x: String = "X"
    override val y: String = "Y"
    val z: String = "Z"
}

class <!JS_EXTERNAL_INHERITORS_ONLY!>ClassXZ<!> : ExternalOpenClassX() {
    val z: String = "Z"
}

class <!JS_EXTERNAL_INHERITORS_ONLY!>ClassXZY<!> : ExternalOpenClassXZ() {
    val y: String = "Y"
}

// check nested

external class ExternalClassNameSpace {
    @OptIn(ExperimentalStdlibApi::class)
    @JsExternalInheritorsOnly
    interface NestedInterfaceX {
        val x: String
    }

    interface NestedInterfaceXY : NestedInterfaceX {
        val y: String
    }
}

external interface ExternalInterfaceXY2 : ExternalClassNameSpace.NestedInterfaceX {
    val y: String
}

external interface ExternalInterfaceXYZ2 : ExternalClassNameSpace.NestedInterfaceXY {
    val z: String
}

interface <!JS_EXTERNAL_INHERITORS_ONLY!>InterfaceXY2<!> : ExternalClassNameSpace.NestedInterfaceX {
    val y: String
}

interface <!JS_EXTERNAL_INHERITORS_ONLY!>InterfaceXYZ2<!> : ExternalClassNameSpace.NestedInterfaceXY {
    val z: String
}

// multiple inheritance

external class ExternalClassXY2 : ExternalInterfaceX, ExternalOpenClassX {
    val y: String
}

class <!JS_EXTERNAL_INHERITORS_ONLY, JS_EXTERNAL_INHERITORS_ONLY!>ClassXY2<!> : ExternalInterfaceX, ExternalOpenClassX() {
    val y: String = "Y"
}
