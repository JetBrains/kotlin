package org.jetbrains.jet.lang.resolve.android

import java.util.ArrayList


open class Context(val buffer: StringBuffer = StringBuffer(), var indentDepth: Int = 0) {
    open class InvalidIndent(num: Int) : RuntimeException("Indentation level < 0: $num")

    val indentUnit = "    "
    protected var currentIndent: String = indentUnit.repeat(indentDepth)
    val children = ArrayList<Context>()

    public fun incIndent() {
        indentDepth++
        currentIndent += indentUnit
    }

    public fun decIndent() {
        indentDepth--
        if (indentDepth < 0)
            throw InvalidIndent(indentDepth)
        currentIndent = currentIndent.substring(0, currentIndent.length - indentUnit.length)
    }

    public open fun write(what: String) {
        writeNoIndent(currentIndent)
        writeNoIndent(what)
    }

    public fun writeNoIndent(what: String) {
        buffer.append(what)
    }

    public fun writeln(what: String) {
        write(what)
        newLine()
    }

    public fun newLine() {
        writeNoIndent("\n")
    }


    public fun trim(num: Int) {
        buffer.delete(buffer.length - num, buffer.length)
    }

    public fun fork(newBuffer: StringBuffer = StringBuffer(),
                    newIndentDepth: Int = indentDepth): Context {
        val child = Context(newBuffer, newIndentDepth)
        children.add(child)
        return child
    }

    public fun adopt<T : Context>(c: T, inheritIndent: Boolean = true): T {
        children.add(c)
        if (inheritIndent) c.currentIndent = currentIndent
        return c
    }

    public fun absorbChildren(noIndent: Boolean = true) {
        for (child in children) {
            child.absorbChildren()
            if (noIndent)
                writeNoIndent(child.toString())
            else
                write(child.toString())
        }
        children.clear()
    }

    public override fun toString(): String {
        return buffer.toString()
    }
}

