// RUN_PIPELINE_TILL: BACKEND
interface Base {
    fun f() = 1
}
    
open class Left() : Base

interface Right : Base

class Diamond() : Left(), Right

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, primaryConstructor */
