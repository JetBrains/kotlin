// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Base<P> {
    fun f() = 1
}
    
open class Left<P>() : Base<P>

interface Right<P> : Base<P>

class Diamond<P>() : Left<P>(), Right<P>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, nullableType,
primaryConstructor, typeParameter */
