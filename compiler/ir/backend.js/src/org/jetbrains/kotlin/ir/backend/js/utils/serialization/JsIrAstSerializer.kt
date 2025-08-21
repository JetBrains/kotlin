/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils.serialization

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrIcClassModel
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragments
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.*

fun JsIrProgramFragments.serializeTo(output: OutputStream) {
    JsIrAstSerializer().append(this).saveTo(output)
}

private class DataWriter {

    val data = ByteArrayOutputStream()
    private val output = DataOutputStream(data)

    fun saveTo(output: DataOutputStream) {
        output.let {
            data.writeTo(it)
        }
    }

    fun writeByte(byte: Int) {
        // Limit bytes to positive values to avoid conversion in deserializer
        if ((byte and 0x7F.inv()) != 0) error("Byte out of bounds: $byte")
        output.writeByte(byte)
    }

    fun writeByteArray(byteArray: ByteArray) {
        output.writeInt(byteArray.size)
        output.write(byteArray)
    }

    fun writeString(string: String) {
        writeByteArray(string.toByteArray(SerializationCharset))
    }

    fun writeBoolean(boolean: Boolean) {
        output.writeBoolean(boolean)
    }

    fun writeInt(int: Int) {
        output.writeInt(int)
    }

    fun writeDouble(double: Double) {
        output.writeDouble(double)
    }

    inline fun <T> writeCollection(collection: Collection<T>, writeItem: (T) -> Unit) {
        output.writeInt(collection.size)
        collection.forEach(writeItem)
    }

    inline fun <T> ifNotNull(t: T?, write: (T) -> Unit): T? {
        output.writeBoolean(t != null)
        if (t != null) {
            write(t)
        }
        return t
    }

    inline fun ifTrue(condition: Boolean, write: () -> Unit) {
        output.writeBoolean(condition)
        if (condition) {
            write()
        }
    }
}

private class JsIrAstSerializer {

    private val fragmentSerializer = DataWriter()
    private val nameSerializer = DataWriter()
    private val stringSerializer = DataWriter()

    private val nameMap = mutableMapOf<JsName, Int>()
    private val stringMap = mutableMapOf<String, Int>()
    private val fileStack: Deque<String> = ArrayDeque()
    private val importedNames = mutableSetOf<JsName>()

    fun append(fragments: JsIrProgramFragments): JsIrAstSerializer {
        append(fragments.mainFragment)
        fragmentSerializer.ifNotNull(fragments.exportFragment, ::append)
        return this
    }

    fun append(fragment: JsIrProgramFragment): JsIrAstSerializer {
        importedNames += fragment.imports.map { fragment.nameBindings[it.key] ?: error("No binding for tag ${it.key}") }
        fragmentSerializer.writeFragment(fragment)
        return this
    }

    fun saveTo(rawOutput: OutputStream) {
        DataOutputStream(rawOutput).use {
            it.writeInt(stringMap.size)
            stringSerializer.saveTo(it)

            it.writeInt(nameMap.size)
            nameSerializer.saveTo(it)

            fragmentSerializer.saveTo(it)
        }
    }

    private fun DataWriter.writeFragment(fragment: JsIrProgramFragment) {
        writeString(fragment.name)
        writeString(fragment.packageFqn)

        writeCollection(fragment.importedModules) {
            writeInt(internalizeString(it.externalName))
            writeInt(internalizeName(it.internalName))
            ifNotNull(it.plainReference) {
                writeExpression(it)
            }
        }

        writeCollection(fragment.imports.entries) { (signatureId, statement) ->
            writeInt(internalizeString(signatureId))
            writeStatement(statement)
        }

        writeCompositeBlock(fragment.declarations)
        writeCompositeBlock(fragment.initializers)
        writeCompositeBlock(fragment.eagerInitializers)
        writeCompositeBlock(fragment.exports)
        writeCompositeBlock(fragment.polyfills)

        writeCollection(fragment.nameBindings.entries) { (key, name) ->
            writeInt(internalizeString(key))
            writeInt(internalizeName(name))
        }

        writeCollection(fragment.optionalCrossModuleImports) {
            writeInt(internalizeString(it))
        }

        writeCollection(fragment.classes.entries) { (name, model) ->
            writeInt(internalizeName(name))
            writeIrIcModel(model)
        }

        ifNotNull(fragment.mainFunctionTag) {
            writeString(it)
        }

        ifNotNull(fragment.testEnvironment) {
            writeInt(internalizeString(it.testFunctionTag))
            writeInt(internalizeString(it.suiteFunctionTag))
        }

        ifNotNull(fragment.dts) {
            writeString(it.raw)
        }

        writeCollection(fragment.definitions) {
            writeInt(internalizeString(it))
        }
    }

    private fun DataWriter.writeIrIcModel(classModel: JsIrIcClassModel) {
        writeCollection(classModel.superClasses) {
            writeInt(internalizeName(it))
        }
        writeCompositeBlock(classModel.preDeclarationBlock)
        writeCompositeBlock(classModel.postDeclarationBlock)
    }

    private fun DataWriter.writeStatement(statement: JsStatement) {
        val visitor = object : JsVisitor() {
            override fun visitReturn(x: JsReturn) {
                writeByte(StatementIds.RETURN)
                ifNotNull(x.expression) {
                    writeExpression(it)
                }
            }

            override fun visitThrow(x: JsThrow) {
                writeByte(StatementIds.THROW)
                writeExpression(x.expression)
            }

            override fun visitBreak(x: JsBreak) {
                writeByte(StatementIds.BREAK)
                ifNotNull(x.label) {
                    writeInt(internalizeName(it.name!!))
                }
            }

            override fun visitContinue(x: JsContinue) {
                writeByte(StatementIds.CONTINUE)
                ifNotNull(x.label) {
                    writeInt(internalizeName(it.name!!))
                }
            }

            override fun visitDebugger(x: JsDebugger) {
                writeByte(StatementIds.DEBUGGER)
            }

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                writeByte(StatementIds.EXPRESSION)
                writeExpression(x.expression)
                ifNotNull(x.exportedTag) { writeInt(internalizeString(it)) }
            }

            override fun visitVars(x: JsVars) {
                writeByte(StatementIds.VARS)
                writeVars(x)
            }

            override fun visitBlock(x: JsBlock) {
                if (x is JsCompositeBlock) {
                    writeByte(StatementIds.COMPOSITE_BLOCK)
                    writeCompositeBlock(x)
                } else {
                    writeByte(StatementIds.BLOCK)
                    writeCollection(x.statements) {
                        writeStatement(it)
                    }
                }
            }

            override fun visitLabel(x: JsLabel) {
                writeByte(StatementIds.LABEL)
                writeInt(internalizeName(x.name))
                writeStatement(x.statement)
            }

            override fun visitIf(x: JsIf) {
                writeByte(StatementIds.IF)
                writeExpression(x.ifExpression)
                writeStatement(x.thenStatement)
                ifNotNull(x.elseStatement) { writeStatement(it) }
            }

            override fun visit(x: JsSwitch) {
                writeByte(StatementIds.SWITCH)
                writeExpression(x.expression)
                writeCollection(x.cases) { case ->
                    withLocation(case) {
                        ifNotNull(case as? JsCase) {
                            writeExpression(it.caseExpression)
                        }
                    }
                    writeCollection(case.statements) { part ->
                        writeStatement(part)
                    }
                }
            }

            override fun visitWhile(x: JsWhile) {
                writeByte(StatementIds.WHILE)
                writeExpression(x.condition)
                writeStatement(x.body)
            }

            override fun visitDoWhile(x: JsDoWhile) {
                writeByte(StatementIds.DO_WHILE)
                writeExpression(x.condition)
                writeStatement(x.body)
            }

            override fun visitFor(x: JsFor) {
                writeByte(StatementIds.FOR)
                ifNotNull(x.condition) { writeExpression(it) }
                ifNotNull(x.incrementExpression) { writeExpression(it) }
                ifNotNull(x.body) { writeStatement(it) }

                ifNotNull(x.initVars) {
                    writeVars(it)
                } ?: ifNotNull(x.initExpression) {
                    writeExpression(it)
                }
            }

            override fun visitForIn(x: JsForIn) {
                writeByte(StatementIds.FOR_IN)
                ifNotNull(x.iterVarName) {
                    writeInt(internalizeName(it))
                }
                ifNotNull(x.iterExpression) { writeExpression(it) }
                writeExpression(x.objectExpression)
                writeStatement(x.body)
            }

            override fun visitTry(x: JsTry) {
                writeByte(StatementIds.TRY)
                writeBlock(x.tryBlock)
                writeCollection(x.catches) { c ->
                    writeInt(internalizeName(c.parameter.name))
                    writeBlock(c.body)
                }
                ifNotNull(x.finallyBlock) { writeBlock(it) }
            }

            override fun visitExport(export: JsExport) {
                writeByte(StatementIds.EXPORT)

                when (val subject = export.subject) {
                    is JsExport.Subject.All -> writeByte(ExportType.ALL)
                    is JsExport.Subject.Elements -> {
                        writeByte(ExportType.ITEMS)
                        writeCollection(subject.elements) {
                            writeInt(internalizeName(it.name.name!!))
                            ifNotNull(it.alias) { writeInt(internalizeName(it)) }
                        }
                    }
                }

                ifNotNull(export.fromModule) { writeString(it) }
            }

            override fun visitImport(import: JsImport) {
                writeByte(StatementIds.IMPORT)
                writeString(import.module)

                when (val target = import.target) {
                    is JsImport.Target.Effect -> {
                        writeByte(ImportType.EFFECT)
                    }
                    is JsImport.Target.All -> {
                        writeByte(ImportType.ALL)
                        writeInt(internalizeName(target.alias.name!!))
                    }
                    is JsImport.Target.Default -> {
                        writeByte(ImportType.DEFAULT)
                        writeInt(internalizeName(target.name.name!!))
                    }
                    is JsImport.Target.Elements -> {
                        writeByte(ImportType.ITEMS)
                        writeCollection(target.elements) {
                            writeInt(internalizeName(it.name))
                            ifNotNull(it.alias) { writeInt(internalizeName(it.name!!)) }
                        }
                    }
                }
            }

            override fun visitEmpty(x: JsEmpty) {
                writeByte(StatementIds.EMPTY)
            }

            override fun visitSingleLineComment(comment: JsSingleLineComment) {
                writeByte(StatementIds.SINGLE_LINE_COMMENT)
                writeString(comment.text)
            }

            override fun visitMultiLineComment(comment: JsMultiLineComment) {
                writeByte(StatementIds.MULTI_LINE_COMMENT)
                writeString(comment.text)
            }

            override fun visitElement(node: JsNode) {
                error("Unknown statement type: ${statement::class.qualifiedName}")
            }
        }

        withComments(statement) {
            withLocation(statement) {
                statement.accept(visitor)
            }
        }

        writeBoolean((statement as HasMetadata).synthetic)
    }

    private fun DataWriter.writeExpression(expression: JsExpression) {
        val visitor = object : JsVisitor() {

            override fun visitThis(x: JsThisRef) {
                writeByte(ExpressionIds.THIS_REF)
            }

            override fun visitSuper(x: JsSuperRef) {
                writeByte(ExpressionIds.SUPER_REF)
            }

            override fun visitNull(x: JsNullLiteral) {
                writeByte(ExpressionIds.NULL)
            }

            override fun visitBoolean(x: JsBooleanLiteral) {
                if (x.value) {
                    writeByte(ExpressionIds.TRUE_LITERAL)
                } else {
                    writeByte(ExpressionIds.FALSE_LITERAL)
                }
            }

            override fun visitString(x: JsStringLiteral) {
                writeByte(ExpressionIds.STRING_LITERAL)
                writeInt(internalizeString(x.value))
            }

            override fun visitRegExp(x: JsRegExp) {
                writeByte(ExpressionIds.REG_EXP)
                writeInt(internalizeString(x.pattern))
                ifNotNull(x.flags) { writeInt(internalizeString(it)) }
            }

            override fun visitInt(x: JsIntLiteral) {
                writeByte(ExpressionIds.INT_LITERAL)
                writeInt(x.value)
            }

            override fun visitBigInt(x: JsBigIntLiteral) {
                writeByte(ExpressionIds.BIGINT_LITERAL)
                writeByteArray(x.value.toByteArray())
            }

            override fun visitDouble(x: JsDoubleLiteral) {
                writeByte(ExpressionIds.DOUBLE_LITERAL)
                writeDouble(x.value)
            }

            override fun visitArray(x: JsArrayLiteral) {
                writeByte(ExpressionIds.ARRAY_LITERAL)
                writeCollection(x.expressions) {
                    writeExpression(it)
                }
            }

            override fun visitObjectLiteral(x: JsObjectLiteral) {
                writeByte(ExpressionIds.OBJECT_LITERAL)
                writeCollection(x.propertyInitializers) {
                    writeExpression(it.labelExpr)
                    writeExpression(it.valueExpr)
                }
                writeBoolean(x.isMultiline)
            }

            override fun visitFunction(x: JsFunction) {
                writeByte(ExpressionIds.FUNCTION)
                writeFunction(x)
            }

            override fun visitClass(x: JsClass) {
                writeByte(ExpressionIds.CLASS)
                ifNotNull(x.name) {
                    writeInt(internalizeName(it))
                }
                ifNotNull(x.baseClass) {
                    writeExpression(it)
                }
                ifNotNull(x.constructor) {
                    writeFunction(it)
                }
                writeCollection(x.members) {
                    writeFunction(it)
                }
            }

            override fun visitDocComment(comment: JsDocComment) {
                writeByte(ExpressionIds.DOC_COMMENT)
                writeCollection(comment.tags.entries) { (name, value) ->
                    writeInt(internalizeString(name))

                    ifNotNull(value as? JsNameRef) {
                        writeExpression(it)
                    } ?: if (value is String) {
                        writeInt(internalizeString(value))
                    } else {
                        error("Unsupported tag: ${value::class.qualifiedName}")
                    }
                }
            }

            override fun visitBinaryExpression(x: JsBinaryOperation) {
                writeByte(ExpressionIds.BINARY_OPERATION)
                writeByte(x.operator.ordinal)
                writeExpression(x.arg1)
                writeExpression(x.arg2)
            }

            override fun visitPrefixOperation(x: JsPrefixOperation) {
                writeByte(ExpressionIds.PREFIX_OPERATION)
                writeByte(x.operator.ordinal)
                writeExpression(x.arg)
            }

            override fun visitPostfixOperation(x: JsPostfixOperation) {
                writeByte(ExpressionIds.POSTFIX_OPERATION)
                writeByte(x.operator.ordinal)
                writeExpression(x.arg)
            }

            override fun visitConditional(x: JsConditional) {
                writeByte(ExpressionIds.CONDITIONAL)
                writeExpression(x.testExpression)
                writeExpression(x.thenExpression)
                writeExpression(x.elseExpression)
            }

            override fun visitArrayAccess(x: JsArrayAccess) {
                writeByte(ExpressionIds.ARRAY_ACCESS)
                writeExpression(x.arrayExpression)
                writeExpression(x.indexExpression)
            }

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                val qualifier = nameRef.qualifier
                if (name != null) {
                    if (qualifier != null || nameRef.isInline == true) {
                        writeByte(ExpressionIds.NAME_REFERENCE)
                        writeInt(internalizeName(name))
                        ifNotNull(qualifier) { writeExpression(it) }
                        ifNotNull(nameRef.isInline) { writeBoolean(it) }
                    } else {
                        writeByte(ExpressionIds.SIMPLE_NAME_REFERENCE)
                        writeInt(internalizeName(name))
                    }
                } else {
                    writeByte(ExpressionIds.PROPERTY_REFERENCE)
                    writeInt(internalizeString(nameRef.ident))
                    ifNotNull(qualifier) { writeExpression(it) }
                    ifNotNull(nameRef.isInline) { writeBoolean(it) }
                }
            }

            override fun visitInvocation(invocation: JsInvocation) {
                writeByte(ExpressionIds.INVOCATION)
                writeExpression(invocation.qualifier)
                writeCollection(invocation.arguments) { writeExpression(it) }
                ifNotNull(invocation.isInline) { writeBoolean(it) }
            }

            override fun visitNew(x: JsNew) {
                writeByte(ExpressionIds.NEW)
                writeExpression(x.constructorExpression)
                writeCollection(x.arguments) { writeExpression(it) }
            }

            override fun visitYield(x: JsYield) {
                writeByte(ExpressionIds.YIELD)
                ifNotNull(x.expression) { writeExpression(it) }
            }
        }

        withComments(expression) {
            withLocation(expression) {
                expression.accept(visitor)
            }
        }

        writeBoolean(expression.synthetic)
        writeByte(expression.sideEffects.ordinal)
        ifNotNull(expression.localAlias) { writeImportedModule(it) }
    }

    private fun DataWriter.writeImportedModule(module: JsImportedModule) {
        writeInt(internalizeString(module.externalName))
        writeInt(internalizeName(module.internalName))
        ifNotNull(module.plainReference) { writeExpression(it) }
    }

    private fun DataWriter.writeFunction(function: JsFunction) {
        writeBlock(function.body)
        writeCollection(function.parameters) { writeParameter(it) }
        writeCollection(function.modifiers) { writeInt(it.ordinal) }
        ifNotNull(function.name) {
            writeInt(internalizeName(it))
        }
        writeBoolean(function.isLocal)
        writeBoolean(function.isEs6Arrow)
    }

    private fun DataWriter.writeParameter(parameter: JsParameter) {
        writeInt(internalizeName(parameter.name))
        writeBoolean(parameter.hasDefaultValue)
    }

    private fun DataWriter.writeCompositeBlock(block: JsCompositeBlock) {
        writeCollection(block.statements) { writeStatement(it) }
    }

    private fun DataWriter.writeBlock(block: JsBlock) {
        writeBoolean(block is JsCompositeBlock)
        writeCollection(block.statements) { writeStatement(it) }
    }

    private fun DataWriter.writeVars(vars: JsVars) {
        writeBoolean(vars.isMultiline)
        writeCollection(vars.vars) { varDecl ->
            withLocation(varDecl) {
                writeInt(internalizeName(varDecl.name))
                ifNotNull(varDecl.initExpression) { writeExpression(it) }
            }
        }
        ifNotNull(vars.exportedPackage) { writeInt(internalizeString(it)) }
    }

    private fun internalizeName(name: JsName): Int = nameMap.getOrPut(name) {
        // Make sure all dependant JsName's are already serialized.
        name.localAlias?.let { internalizeName(it.name) }

        nameSerializer.apply {
            writeInt(internalizeString(name.ident))
            writeBoolean(name.isTemporary)
            ifNotNull(name.localAlias) { writeLocalAlias(it) }
            writeBoolean(name.imported && name !in importedNames)
            writeBoolean(name.constant)
            ifNotNull(name.specialFunction) { writeByte(it.ordinal) }
        }
        nameMap.size
    }

    private fun DataWriter.writeLocalAlias(alias: LocalAlias) {
        writeInt(internalizeName(alias.name))
        ifNotNull(alias.tag) { writeInt(internalizeString(it)) }
    }

    private fun internalizeString(string: String) = stringMap.getOrPut(string) {
        stringSerializer.writeString(string)
        stringMap.size
    }

    private fun DataWriter.writeComment(comment: JsComment) {
        writeString(comment.text)
        when (comment) {
            is JsSingleLineComment -> writeBoolean(false)
            is JsMultiLineComment -> writeBoolean(true)
            else -> error("Unknown type of comment ${comment.javaClass.name}")
        }
    }

    private inline fun DataWriter.withLocation(node: JsNode, inner: () -> Unit) {
        val location = node.source.safeAs<JsLocationWithSource>()?.asSimpleLocation()
        ifNotNull(location) {
            val lastFile = fileStack.peek()
            val newFile = it.file
            val fileChanged = lastFile != newFile
            ifTrue(fileChanged) {
                writeInt(internalizeString(newFile))
                fileStack.push(it.file)
            }
            writeInt(it.startLine)
            writeInt(it.startChar)

            inner()

            if (fileChanged) {
                fileStack.pop()
            }
        } ?: inner()
    }

    private inline fun DataWriter.withComments(node: JsNode, inner: () -> Unit) {
        inner()
        ifNotNull(node.commentsBeforeNode) { comments -> writeCollection(comments) { writeComment(it) } }
        ifNotNull(node.commentsAfterNode) { comments -> writeCollection(comments) { writeComment(it) } }
    }
}
