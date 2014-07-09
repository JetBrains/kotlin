package org.jetbrains.jet.lang.resolve.android

trait KotlinWriter

class KotlinStringWriter : KotlinWriter {

    val ctx = Context()

    fun writeFunction(name: String,
                      args: Collection<String>?,
                      retType: String,
                      body: Collection<String>) {
        val returnTerm = if (retType == "" || retType == "Unit") "" else ": $retType"
        val argStr = if (args != null) args.join(", ") else ""
        ctx.writeln("fun $name($argStr)$returnTerm {")
        ctx.incIndent()
        for (stmt in body)
            ctx.writeln(stmt)
        ctx.decIndent()
        ctx.writeln("}")
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
        ctx.writeln("val $name: $retType")
        ctx.incIndent()
        ctx.write("get() ")
        if (getterBody.size > 1) {
            ctx.writeNoIndent("{\n")
            ctx.incIndent()
            for (stmt in getterBody) {
                ctx.writeln(stmt)
            }
            ctx.decIndent()
            ctx.writeln("}")
        } else {
            ctx.writeNoIndent("=")
            ctx.writeNoIndent(getterBody.join("").replace("return", ""))
            ctx.newLine()
        }
        ctx.decIndent()
        ctx.newLine()
    }

    fun writeImmutableExtensionProperty(receiver: String,
                                        name: String,
                                        retType: String,
                                        getterBody: Collection<String>) {
        writeImmutableProperty("$receiver.$name", retType, getterBody)
    }

    fun writeImport(what: String) {
        ctx.writeln("import $what")
    }

    fun writeEmptyLine() {
        ctx.newLine()
    }

    fun output() = ctx.buffer
}

