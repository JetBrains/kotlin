// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// MODULE: dependency

package dependency

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun f() = dependency.DPoint(1.0, 2.0)
inline fun inlined() = dependency.DPoint(1.0, 2.0)

fun id(x: dependency.DPoint) = x
inline fun idInlined(x: dependency.DPoint) = x


// MODULE: main(dependency)

package main

fun f() = dependency.DPoint(1.0, 2.0)
inline fun inlined() = dependency.DPoint(1.0, 2.0)
fun id(x: dependency.DPoint) = x
inline fun idInlined(x: dependency.DPoint) = x



fun box(): String {
    if (f().toString() != "DPoint(x=1.0, y=2.0)") return f().toString()
    if (inlined().toString() != "DPoint(x=1.0, y=2.0)") return inlined().toString()
    if (f() != f()) return f().toString()
    if (f() != inlined()) return "${f()} ${inlined()}"
    if (inlined() != f()) return "${inlined()} ${f()}"
    if (inlined() != inlined()) return inlined().toString()
    if (id(f()) != f()) return id(f()).toString()
    if (idInlined(f()) != f()) return id(f()).toString()

    if (dependency.f().toString() != "DPoint(x=1.0, y=2.0)") return dependency.f().toString()
    if (dependency.inlined().toString() != "DPoint(x=1.0, y=2.0)") return dependency.inlined().toString()
    if (dependency.f() != dependency.f()) return dependency.f().toString()
    if (dependency.f() != dependency.inlined()) return "${dependency.f()} ${dependency.inlined()}"
    if (dependency.inlined() != dependency.f()) return "${dependency.inlined()} ${dependency.f()}"
    if (dependency.inlined() != dependency.inlined()) return dependency.inlined().toString()
    if (dependency.id(dependency.f()) != dependency.f()) return dependency.id(dependency.f()).toString()
    if (dependency.idInlined(dependency.f()) != dependency.f()) return dependency.id(dependency.f()).toString()
    return "OK"
}
