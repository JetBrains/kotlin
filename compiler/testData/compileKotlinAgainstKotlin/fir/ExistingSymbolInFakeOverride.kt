// TARGET_BACKEND: JVM
// FILE: A.kt

interface KotlinMangler<D : Any> {
    val String.hashMangle: Long
    val D.fqnString: String
    val D.fqnMangle: Long get() = fqnString.hashMangle
    val manglerName: String

    interface IrMangler : KotlinMangler<String> {
        override val manglerName: String
            get() = "Ir"
    }
}

abstract class AbstractKotlinMangler<D : Any> : KotlinMangler<D> {
    override val String.hashMangle get() = 42L
}

abstract class IrBasedKotlinManglerImpl : AbstractKotlinMangler<String>(), KotlinMangler.IrMangler {
    override val String.fqnString: String
        get() = this
}

// FILE: B.kt

abstract class AbstractJvmManglerIr : IrBasedKotlinManglerImpl()

object JvmManglerIr : AbstractJvmManglerIr()

fun box() = "OK"