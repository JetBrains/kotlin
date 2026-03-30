// RUN_PIPELINE_TILL: BACKEND
import kotlin.jvm.*

interface Base {
    fun foo()
}

class Derived : Base {
    override external fun foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, external, functionDeclaration, interfaceDeclaration, override */
