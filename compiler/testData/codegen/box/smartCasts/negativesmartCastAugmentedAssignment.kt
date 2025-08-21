// WITH_STDLIB

interface IrMutableAnnotationContainer {
    var annotations: List<String>
}

interface IrDeclaration : IrMutableAnnotationContainer

abstract class IrProperty : IrDeclaration

fun foo(declaration: IrDeclaration) {
    if (declaration is IrProperty) return
    declaration.annotations += listOf("OK")
}

fun box(): String {
    val d = object : IrDeclaration {
        override var annotations: List<String> = listOf()
    }
    foo(d)
    return d.annotations[0]
}