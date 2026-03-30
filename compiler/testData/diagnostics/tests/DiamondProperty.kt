// RUN_PIPELINE_TILL: BACKEND
interface Base {
    var v : Int
        get() = 1
        set(v) {}
}

open class Left() : Base

interface Right : Base

class Diamond() : Left(), Right

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, interfaceDeclaration, primaryConstructor,
propertyDeclaration, setter */
