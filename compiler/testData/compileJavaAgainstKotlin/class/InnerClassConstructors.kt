// IGNORE_BACKEND: JVM_IR
package test

class Outer {
    inner class InnerGeneric(param: List<String>)

    inner class InnerPrimitive(param: Int)
}