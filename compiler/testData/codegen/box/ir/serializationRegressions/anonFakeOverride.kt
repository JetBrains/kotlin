// KJS_WITH_FULL_RUNTIME

// extracted case from KT-48912

// MODULE: libxxxxx
// FILE: l1.kt

package com.soywiz.korag.shader

open class Operand

open class Variable : Operand()

class Program {
    class Stm(val o: Operand)

    open class Visitor<E>(val default: E) {
        open fun visit(stm: Stm?) {
            if (stm?.o is Operand) visit(stm.o)
        }
        open fun visit(o: Operand): E {
            if (o is Uniform) visit(o)
            return default
        }
        open fun visit(operand: Uniform): E = default
    }
}

open class Uniform(val result: String): Variable()

open class Shader(val stm: Program.Stm?) {

    val uniforms = LinkedHashSet<Uniform>().also { out ->
        object : Program.Visitor<Unit>(Unit) {
            override fun visit(uniform: Uniform) {
                out += uniform
            }
        }.visit(stm)
    }.toSet()
}

// MODULE: lib2(libxxxxx)
// FILE: l2.kt

import com.soywiz.korag.shader.*

class D : Shader(Program.Stm((Uniform("OK"))))

fun foo(): Shader = D()


// MODULE: main(lib2)
// FILE: m1.kt

fun box(): String {
    return foo().uniforms.first().result
}