/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

/**
 * Allows to nest declaration mangles.
 */
sealed class ManglingContext {

    abstract val prefix: String

    class Module(name: String) : ManglingContext() {
        override val prefix: String = "$name."
    }

    /**
     * Used to represent containers like structures, classes, etc.
     */
    class Entity(name: String, parentContext: ManglingContext = Empty) : ManglingContext() {
        override val prefix: String = "${parentContext.prefix}$name."
    }

    object Empty : ManglingContext() {
        override val prefix: String = ""
    }
}

/**
 * We need a way to refer external declarations from Kotlin Libraries
 * by stable unique identifier. To be able to do it, we mangle them.
 */
interface InteropMangler {
    val StructDecl.uniqueSymbolName: String
    val EnumDef.uniqueSymbolName: String
    val EnumConstant.uniqSymbolName: String
    val ObjCClass.uniqueSymbolName: String
    val ObjCClass.metaClassUniqueSymbolName: String
    val ObjCProtocol.uniqueSymbolName: String
    val ObjCProtocol.metaClassUniqueSymbolName: String
    val ObjCMethod.uniqueSymbolName: String
    val ObjCProperty.uniqueSymbolName: String
    val TypedefDef.uniqueSymbolName: String
    val FunctionDecl.uniqueSymbolName: String
    val ConstantDef.uniqueSymbolName: String
    val WrappedMacroDef.uniqueSymbolName: String
    val GlobalDecl.uniqueSymbolName: String
}

/**
 * Mangler that mimics behaviour of the one from the Kotlin compiler.
 */
class KotlinLikeInteropMangler(context: ManglingContext = ManglingContext.Empty) : InteropMangler {

    private val prefix = context.prefix

    override val StructDecl.uniqueSymbolName: String
        get() = "structdecl:$prefix$spelling"

    override val EnumDef.uniqueSymbolName: String
        get() = "enumdef:$prefix$spelling"

    override val EnumConstant.uniqSymbolName: String
        get() = "enumconstant:$prefix$name"

    override val ObjCClass.uniqueSymbolName: String
        get() = "objcclass:$prefix$name"

    override val ObjCClass.metaClassUniqueSymbolName: String
        get() = "objcmetaclass:$prefix$name"

    override val ObjCProtocol.uniqueSymbolName: String
        get() = "objcprotocol:$prefix$name"

    override val ObjCProtocol.metaClassUniqueSymbolName: String
        get() = "objcmetaprotocol:$prefix$name"

    override val ObjCMethod.uniqueSymbolName: String
        get() = "objcmethod:$prefix$selector"

    override val ObjCProperty.uniqueSymbolName: String
        get() = "objcproperty:$prefix$name"

    override val TypedefDef.uniqueSymbolName: String
        get() = "typedef:$prefix$name"

    override val FunctionDecl.uniqueSymbolName: String
        get() = "funcdecl:$prefix$functionName"

    override val ConstantDef.uniqueSymbolName: String
        get() = "macrodef:$prefix$name"

    override val WrappedMacroDef.uniqueSymbolName: String
        get() = "macrodef:$prefix$name"

    override val GlobalDecl.uniqueSymbolName: String
        get() = "globaldecl:$prefix$name"

    private val FunctionDecl.functionName: String
        get() = name
}