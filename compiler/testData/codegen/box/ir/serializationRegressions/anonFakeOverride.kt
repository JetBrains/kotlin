// KJS_WITH_FULL_RUNTIME
// WITH_STDLIB

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

    // Here for object.visit(Program.Stm?) FO were 2 different mangles
    // 1.5.31: <anonymous>#static(kotlin.collections.LinkedHashSet<com.soywiz.korag.shader.Uniform>){}visit(com.soywiz.korag.shader.Program.Stm?){}
    // 1.6.0+: {}uniforms<anonymous>#static(kotlin.collections.LinkedHashSet<com.soywiz.korag.shader.Uniform>){}visit(com.soywiz.korag.shader.Program.Stm?){}
    // So fix is to make sure those mangles are identical.

    val uniforms = LinkedHashSet<Uniform>().also { out ->
        object : Program.Visitor<Unit>(Unit) {
            override fun visit(uniform: Uniform) {
                out.add(uniform)
            }
        }.visit(stm)
    }.toSet()
}

// MODULE: main(libxxxxx)
// FILE: l2.kt

import com.soywiz.korag.shader.*

class D : Shader(Program.Stm((Uniform("OK"))))

fun foo(): Shader = D()

fun box(): String {
    return foo().uniforms.first().result
}