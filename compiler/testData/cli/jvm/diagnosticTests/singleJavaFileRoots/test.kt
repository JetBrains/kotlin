import lib.*
import lib.ext.*
import lib.classKinds.*

fun main(args: Array<String>) {
    A().b()
    B().c()
    C().a()

    DefaultPackage()

    PackageLocal1()
    PackageLocal2()
}

@AnnotationClass(EnumClass.ENTRY)
fun classKinds(
        c: ClassClass,
        i: InterfaceClass,
        e: EnumClass
) {}
