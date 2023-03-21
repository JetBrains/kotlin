// IGNORE_FIR

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

interface InterfaceXY : ExternalInterfaceX {
    val y: String
}

interface InterfaceXYZ : ExternalInterfaceXY {
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

object ObjectXY : ExternalInterfaceX {
    override val x: String = "X"
    val y: String = "Y"
}

object ObjectXYZ : ExternalInterfaceXY {
    override val x: String = "X"
    override val y: String = "Y"
    val z: String = "Z"
}

object ObjectXZ : ExternalOpenClassX() {
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

class ClassXY : ExternalInterfaceX {
    override val x: String = "X"
    val y: String = "Y"
}

class ClassXYZ : ExternalInterfaceXY {
    override val x: String = "X"
    override val y: String = "Y"
    val z: String = "Z"
}

class ClassXZ : ExternalOpenClassX() {
    val z: String = "Z"
}

class ClassXZY : ExternalOpenClassXZ() {
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

interface InterfaceXY2 : ExternalClassNameSpace.NestedInterfaceX {
    val y: String
}

interface InterfaceXYZ2 : ExternalClassNameSpace.NestedInterfaceXY {
    val z: String
}

// multiple inheritance

external class ExternalClassXY2 : ExternalInterfaceX, ExternalOpenClassX {
    val y: String
}

class ClassXY2 : ExternalInterfaceX, ExternalOpenClassX() {
    val y: String = "Y"
}
