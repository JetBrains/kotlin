package org.jetbrains.jet.lang.resolve.android

trait KotlinWriter {
    fun output(): StringBuffer
}

class KotlinStringWriter : KotlinWriter {

    val ctx = Context()
    val imports = ctx.fork()
    val body = ctx.fork()

    fun writeFunction(name: String,
                      args: Collection<String>?,
                      retType: String,
                      stmts: Collection<String>) {
        val returnTerm = if (retType == "" || retType == "Unit") "" else ": $retType"
        val argStr = if (args != null) args.join(", ") else ""
        body.writeln("fun $name($argStr)$returnTerm {")
        body.incIndent()
        for (stmt in stmts)
            body.writeln(stmt)
        body.decIndent()
        body.writeln("}")
    }

    fun writeExtensionFunction(receiver: String,
                               name: String,
                               args: Collection<String>?,
                               retType: String,
                               body: Collection<String>) {
        writeFunction("$receiver.$name", args, retType, body)
    }

    fun writeImmutableProperty(name: String,
                               retType: String,
                               getterBody: Collection<String>) {
        body.writeln("val $name: $retType")
        body.incIndent()
        body.write("get() ")
        if (getterBody.size > 1) {
            body.writeNoIndent("{\n")
            body.incIndent()
            for (stmt in getterBody) {
                body.writeln(stmt)
            }
            body.decIndent()
            body.writeln("}")
        } else {
            body.writeNoIndent("=")
            body.writeNoIndent(getterBody.join("").replace("return", ""))
            body.newLine()
        }
        body.decIndent()
        body.newLine()
    }

    fun writeImmutableExtensionProperty(receiver: String,
                                        name: String,
                                        retType: String,
                                        getterBody: Collection<String>) {
        writeImmutableProperty("$receiver.$name", retType, getterBody)
    }

    fun writeImport(what: String) {
        imports.writeln("import $what")
    }

    fun writeEmptyLine() {
        body.newLine()
    }

    override fun output(): StringBuffer {
        ctx.absorbChildren()
        return ctx.buffer
    }
}

