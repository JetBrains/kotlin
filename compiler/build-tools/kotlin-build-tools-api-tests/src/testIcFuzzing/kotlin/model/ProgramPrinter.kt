/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.model

/**
 * Prints a [Program] into Kotlin sources. The output is a deterministic function of the model.
 */
class ProgramPrinter private constructor(private val program: Program) {
    private lateinit var currentFile: SourceFile
    private val imports = sortedSetOf<String>()

    private fun printFile(file: SourceFile): String {
        currentFile = file
        imports.clear()
        // a nested class is printed in its outer class
        val decls = file.decls.filterNot { it is ClassDecl && outerOf(it) != null }
        val body = decls.joinToString("\n") { "\n".repeat(file.blankLines[it.id] ?: 0) + printDecl(it) }
        return buildString {
            if (file.commentVariant != 0) appendLine("// comment variant ${file.commentVariant}")
            if (file.jvmNameVariant != 0) {
                appendLine("@file:JvmName(\"Facade${file.jvmNameVariant}\")")
                if (file.multifile) appendLine("@file:JvmMultifileClass")
            }
            appendLine("package ${file.pkg}")
            appendLine()
            if (imports.isNotEmpty()) {
                imports.forEach { appendLine("import $it") }
                appendLine()
            }
            append(body)
        }
    }

    private fun printDecl(decl: Decl): String = when (decl) {
        is FunDecl -> printFun(decl)
        is PropDecl -> printProp(decl)
        is TypeAliasDecl -> "${decl.visibility.keyword}typealias ${decl.name} = ${type(decl.target)}\n"
        is ClassDecl -> printClass(decl)
    }

    private fun printFun(decl: FunDecl): String = buildString {
        if (decl.deprecationConstId != null || decl.hidden) {
            val message = decl.deprecationConstId?.let { constId -> "\${${program.decl(constId)?.let(::reference) ?: unresolved(constId)}}" } ?: "h"
            appendLine("@Deprecated(\"$message\"${if (decl.hidden) ", level = DeprecationLevel.HIDDEN" else ""})")
        }
        // the overloads have the same name, so the JVM name is unique only with the id
        if (decl.jvmNameVariant != 0) appendLine("@JvmName(\"j${decl.id}v${decl.jvmNameVariant}\")")
        if (decl.jvmOverloads && !decl.inline && decl.params.any(decl::printsDefault)) appendLine("@JvmOverloads")
        if (decl.publishedApi && decl.visibility == Visibility.INTERNAL) appendLine("@PublishedApi")
        if (decl.hasContract) appendLine("@OptIn(kotlin.contracts.ExperimentalContracts::class)")
        if (decl.jvmSynthetic) appendLine("@JvmSynthetic")
        if (decl.throws) appendLine("@Throws(IllegalStateException::class)")
        append(decl.visibility.keyword)
        if (decl.inline) append("inline ")
        if (decl.infix && decl.isBinary) append("infix ")
        if (decl.operator && decl.isBinary) append("operator ")
        if (decl.isSuspend) append("suspend ")
        append("fun ")
        decl.typeParam?.let { append("<${if (decl.isReified) "reified " else ""}T${if (decl.typeParamNonNull) " : Any" else ""}> ") }
        decl.receiverClassId?.let { append("${classReference(it)}.") }
        append("${decl.name}(")
        val params = parameters(decl.params, decl::printsDefault)
        val lambdaParam = decl.lambda?.let { kind ->
            val modifier = when {
                !decl.inline -> ""
                kind == LambdaKind.NOINLINE && KnownIssues.inlineOfSeparateCompilation -> "noinline "
                kind == LambdaKind.CROSSINLINE -> "crossinline "
                else -> ""
            }
            "$modifier$LAMBDA_PARAM: ${decl.lambdaShape.type}"
        }
        append((params + listOfNotNull(lambdaParam)).joinToString())
        // an inferred return type is the type of the last expression of the `run` lambda, the statements are kept;
        // a contract has to be the first statement of a block body; the `typeOf` call of a reified function is inlined through the `run` lambda,
        // see KnownIssues.inlineOfSeparateCompilation
        val implicitReturnType = decl.implicitReturnType && !decl.hasContract && isInferable(decl.returnType, decl.bodyVariant) &&
                (!decl.isReified || KnownIssues.inlineOfSeparateCompilation)
        appendLine(if (implicitReturnType) ") = run {" else "): ${type(decl.returnType)} {")
        if (decl.hasContract) appendLine("    kotlin.contracts.contract { returns() implies (${decl.params.first().name} != null) }")
        val extraStatements = buildList {
            val invocation = lambdaInvocation(decl)
            when {
                invocation == null -> {}
                // a crossinline lambda is called from an object, which is a class owned by the inline function copied to the call sites
                decl.inline && decl.lambda == LambdaKind.CROSSINLINE && KnownIssues.inlineFunOwnedClass -> {
                    if (decl.lambdaShape != LambdaShape.SUSPEND) add("object : Runnable { override fun run() { println($invocation) } }.run()")
                }
                else -> add("println($invocation)")
            }
            // the dead branch declares a local variable, see KnownIssues.inlineOfSeparateCompilation
            if (decl.deadBranchVariant != 0 && (!decl.inline || KnownIssues.inlineOfSeparateCompilation)) {
                add("val dead = false")
                add("if (dead) println(\"d${decl.deadBranchVariant}\")")
            }
            if (decl.isReified) add("println(kotlin.reflect.typeOf<T>())")
            // an object using a reified type parameter is copied to the call sites, see KnownIssues.inlineFunOwnedClass
            val reifiedType = if (decl.isReified && KnownIssues.inlineFunOwnedClass) " + kotlin.reflect.typeOf<T>()" else ""
            if (decl.ownedObjectVariant != 0) add("println(object { override fun toString() = \"o${decl.ownedObjectVariant}\"$reifiedType })")
            // local declarations are not allowed in inline functions
            if (!decl.inline && decl.localClassVariant != 0) {
                add("class L { override fun toString() = \"l${decl.localClassVariant}\" }")
                add("println(L())")
            }
            if (!decl.inline && decl.localFunVariant != 0) {
                add("fun lf() = \"f${decl.localFunVariant}\"")
                add("println(lf())")
            }
        }
        // the code inlined through another inline function or an inline property differs between the separate and the joint compilation
        val uses = if (!decl.inline || KnownIssues.inlineOfSeparateCompilation) decl.uses else decl.uses.filterNot { use ->
            when (use) {
                is Use.CallFun -> (program.decl(use.funId) as? FunDecl)?.inline == true
                is Use.ContractSmartCast -> (program.decl(use.funId) as? FunDecl)?.inline == true
                is Use.ReadProp -> isInlineProperty(use.propId)
                is Use.WhenConst -> isInlineProperty(use.propId)
                else -> false
            }
        }
        printBody(uses, decl.returnType, decl.bodyVariant, extraStatements = extraStatements, implicitResult = implicitReturnType)
        appendLine("}")
    }

    private fun printProp(decl: PropDecl): String {
        val receiver = decl.printedReceiverId?.let { "${classReference(it)}." } ?: ""
        val accessor = when {
            decl.kind == PropKind.CONST -> PropAccessor.FIELD
            decl.printedReceiverId != null && (decl.accessor == PropAccessor.FIELD || decl.accessor == PropAccessor.LAZY) -> PropAccessor.GETTER
            decl.accessor == PropAccessor.LAZY && decl.kind != PropKind.VAL -> PropAccessor.FIELD
            else -> decl.accessor
        }
        val lateinit = decl.kind == PropKind.LATEINIT && accessor == PropAccessor.FIELD && decl.printedReceiverId == null && allowsLateinit(decl.type)
        val keyword = when (decl.kind) {
            PropKind.VAL -> "val"
            PropKind.VAR -> "var"
            PropKind.CONST -> "const val"
            PropKind.LATEINIT -> if (lateinit) "lateinit var" else "var"
        }
        val isVar = keyword.endsWith("var")
        val jvmField = decl.jvmField && accessor == PropAccessor.FIELD && decl.printedReceiverId == null && !lateinit &&
                decl.kind != PropKind.CONST && !isValueClassType(decl.type)
        val volatile = decl.volatile && isVar && !lateinit && accessor == PropAccessor.FIELD && decl.printedReceiverId == null
        val annotation = (if (jvmField) "@JvmField " else "") + if (volatile) "@Volatile " else ""
        val inline = if (accessor == PropAccessor.INLINE_GETTER) "inline " else ""
        val typeAnnotation = if (!lateinit && decl.implicitType && isInferable(decl.type, decl.valueVariant)) "" else ": ${type(decl.type)}"
        val value = value(decl.type, decl.valueVariant)
        val declaration = "$annotation${decl.visibility.keyword}$inline$keyword $receiver${decl.name}$typeAnnotation"
        return when {
            lateinit -> "$declaration\n"
            accessor == PropAccessor.FIELD -> "$declaration = $value\n"
            accessor == PropAccessor.LAZY -> "$declaration by lazy { $value }\n"
            else -> "$declaration\n    get() = $value\n" + if (isVar) "    set(value) {}\n" else ""
        }
    }

    private fun isInlineProperty(propId: Int): Boolean =
        (program.decl(propId) as? PropDecl)?.let { it.accessor == PropAccessor.INLINE_GETTER && it.kind != PropKind.CONST } == true

    /** A `lateinit` property has a non-nullable type which is not primitive. */
    private fun allowsLateinit(type: TypeRef): Boolean = !type.nullable && when (type) {
        is TypeRef.BuiltinType -> type.builtin == Builtin.STRING || type.builtin == Builtin.ANY
        is TypeRef.ClassType -> program.classDecl(type.classId)?.kind != ClassKind.VALUE
        is TypeRef.AliasType, is TypeRef.TypeParamType -> false
    }

    /** Whether [type] is a value class type, possibly through typealiases. */
    private fun isValueClassType(type: TypeRef, depth: Int = 0): Boolean = depth <= MAX_ALIAS_DEPTH && when (type) {
        is TypeRef.ClassType -> program.classDecl(type.classId)?.kind == ClassKind.VALUE
        is TypeRef.AliasType -> (program.decl(type.aliasId) as? TypeAliasDecl)?.let { isValueClassType(it.target, depth + 1) } == true
        is TypeRef.BuiltinType, is TypeRef.TypeParamType -> false
    }

    /** A read of [prop], on an instance of its receiver for an extension property. */
    private fun propertyRead(prop: PropDecl): String = (prop.printedReceiverId?.let { "${instance(it)}." } ?: "") + reference(prop)

    /** Whether the value of [type] has a type which may be inferred: `TODO()` and `null` would be inferred as `Nothing` and `Nothing?`. */
    private fun isInferable(type: TypeRef, variant: Int): Boolean = value(type, variant).let { it != "TODO()" && it != "null" }

    private fun printClass(decl: ClassDecl): String = buildString {
        if (decl.kind == ClassKind.VALUE) appendLine("@JvmInline")
        append(decl.visibility.keyword)
        when {
            isInner(decl) -> append("inner ")
            isCompanion(decl) -> append("companion ")
        }
        append(
            when (decl.kind) {
                ClassKind.FINAL -> "class "
                ClassKind.OPEN -> "open class "
                ClassKind.ABSTRACT -> "abstract class "
                ClassKind.SEALED -> "sealed class "
                ClassKind.INTERFACE -> if (decl.funInterface && decl.samMember != null) "fun interface " else "interface "
                ClassKind.SEALED_INTERFACE -> "sealed interface "
                ClassKind.DATA -> "data class "
                ClassKind.OBJECT -> "object "
                ClassKind.ENUM -> "enum class "
                ClassKind.VALUE -> "value class "
            }
        )
        append(decl.name)
        when {
            decl.kind == ClassKind.DATA -> append("(val v: Int = 0)")
            decl.kind == ClassKind.VALUE -> append("(val v: Int)")
            decl.kind == ClassKind.ENUM && decl.enumConstructorVariant != 0 -> append("(val v: Int)")
        }
        decl.superId?.let { superId ->
            val superClass = program.classDecl(superId)
            append(" : ")
            append(classReference(superId))
            if (superClass != null && !superClass.kind.isInterface && !decl.kind.isInterface) append("()")
        }
        if (decl.interfaceIds.isNotEmpty()) {
            append(if (decl.superId == null) " : " else ", ")
            append(decl.interfaceIds.joinToString { classReference(it) })
        }
        appendLine(" {")
        if (decl.kind == ClassKind.ENUM) {
            val entries = decl.enumEntries.mapIndexed { index, entry ->
                val args = if (decl.enumConstructorVariant != 0) "(${decl.enumConstructorVariant + index})" else ""
                val body = if (decl.enumEntryBodyVariant != 0) " { override fun toString() = \"b${decl.enumEntryBodyVariant}\" }" else ""
                "$entry$args$body"
            }
            appendLine("    ${entries.joinToString()};")
        }
        for (member in decl.members) {
            printMember(decl, member)
        }
        for (nested in program.classes.filter { outerOf(it) == decl }) {
            append(printClass(nested).prependIndent("    "))
            appendLine()
        }
        appendLine("}")
    }

    /** The class [cls] is declared in, `null` for a top-level class or if its outer class does not exist. */
    private fun outerOf(cls: ClassDecl): ClassDecl? = cls.outerId?.let(program::classDecl)

    /** An inner class is printed as a nested one where an inner class is not allowed. */
    private fun isInner(cls: ClassDecl): Boolean =
        cls.nesting == Nesting.INNER && cls.kind in INNER_KINDS && outerOf(cls)?.kind?.isRegularClass == true

    /** A companion object is printed as a nested object where a companion object is not allowed, or if the class already has one. */
    private fun isCompanion(cls: ClassDecl): Boolean {
        val outer = outerOf(cls) ?: return false
        if (cls.nesting != Nesting.COMPANION || cls.kind != ClassKind.OBJECT) return false
        if (!outer.kind.isRegularClass && !outer.kind.isInterface && outer.kind != ClassKind.ENUM) return false
        return program.classes.first { outerOf(it) == outer && it.nesting == Nesting.COMPANION && it.kind == ClassKind.OBJECT } == cls
    }

    private fun StringBuilder.printMember(owner: ClassDecl, member: MemberFun) {
        val signatureSource = member.overrides?.let(::rootOverridden) ?: member
        val modifiers = when {
            member.overrides != null && member.finalOverride -> "final override "
            member.overrides != null -> "override "
            owner.kind.isInterface -> ""
            member.modality == Modality.OPEN -> "open "
            member.modality == Modality.ABSTRACT -> "abstract "
            else -> ""
        }
        // an override has the visibility and the default values of the overridden member
        val jvmStatic = if (member.jvmStatic && owner.kind == ClassKind.OBJECT && member.overrides == null && member.modality == Modality.FINAL) "@JvmStatic " else ""
        val returnType = signatureSource.returnType.let { if (member.overrides != null && member.covariantReturnType) it.withNullable(false) else it }
        val isProperty = signatureSource.isProperty != (member.overrides != null && member.overridesWithOtherKind)
        val isAbstract = member.modality == Modality.ABSTRACT && member.overrides == null
        // a property of an interface or of a value class has no field, a constant is allowed only in an object
        val accessor = if (owner.kind.isInterface || owner.kind == ClassKind.VALUE) PropAccessor.GETTER else member.accessor
        val isConst = signatureSource.propKind == PropKind.CONST && owner.kind == ClassKind.OBJECT && member.overrides == null &&
                member.modality == Modality.FINAL && accessor == PropAccessor.FIELD && isConstType(returnType)
        val isLateinit = signatureSource.propKind == PropKind.LATEINIT && member.overrides == null && !isAbstract &&
                accessor == PropAccessor.FIELD && allowsLateinit(returnType)
        val isVar = signatureSource.propKind == PropKind.VAR || signatureSource.propKind == PropKind.LATEINIT
        val jvmField = member.jvmField && accessor == PropAccessor.FIELD && member.overrides == null && member.modality == Modality.FINAL &&
                !isConst && !isLateinit && !isValueClassType(returnType)
        val annotations = jvmStatic + if (jvmField) "@JvmField " else ""
        val visibility = annotations + if (member.overrides == null) member.visibility.keyword else ""
        if (isProperty) {
            val keyword = when {
                isConst -> "const val"
                isLateinit -> "lateinit var"
                isVar -> "var"
                else -> "val"
            }
            append("    $visibility$modifiers$keyword ${signatureSource.name}: ${type(returnType)}")
        } else {
            val params = parameters(signatureSource.params) { member.overrides == null && it.hasDefault }
            append("    $visibility${modifiers}fun ${signatureSource.name}(${params.joinToString()}): ${type(returnType)}")
        }
        if (isAbstract || isProperty && isLateinit) {
            appendLine()
            return
        }
        if (isProperty && accessor == PropAccessor.FIELD) {
            appendLine(" = ${value(returnType, member.bodyVariant)}")
        } else if (isProperty) {
            appendLine()
            appendLine("        get() {")
            printBody(member.uses, returnType, member.bodyVariant, indent = "            ")
            appendLine("        }")
            if (isVar) appendLine("        set(value) {}")
        } else {
            appendLine(" {")
            printBody(member.uses, returnType, member.bodyVariant, indent = "        ")
            appendLine("    }")
        }
    }

    /** A constant has a non-nullable primitive or `String` type. */
    private fun isConstType(type: TypeRef): Boolean = type is TypeRef.BuiltinType && !type.nullable && type.builtin != Builtin.ANY

    /** A smart cast of [read] of the nullable [type] to non-null, which compiles only while [read] is stable. */
    private fun smartCast(read: String, type: TypeRef): String =
        "run { if ($read != null) { val y: ${type(type.withNullable(false))} = $read; y } else null }"

    /** The call of the lambda parameter of [decl], `null` if it has none or a suspend lambda cannot be called. */
    private fun lambdaInvocation(decl: FunDecl): String? = decl.lambda?.let {
        when (decl.lambdaShape) {
            LambdaShape.NO_PARAMS -> "$LAMBDA_PARAM()"
            LambdaShape.PARAM -> "$LAMBDA_PARAM(0)"
            LambdaShape.RECEIVER -> "0.$LAMBDA_PARAM()"
            LambdaShape.SUSPEND -> if (decl.isSuspend) "$LAMBDA_PARAM()" else null
        }
    }

    private fun parameters(params: List<Param>, printsDefault: (Param) -> Boolean): List<String> = params.map { param ->
        "${param.name}: ${type(param.type)}" + if (printsDefault(param)) " = ${value(param.type, param.defaultVariant)}" else ""
    }

    /**
     * The arguments of a call passing the [params] without a printed default value, by the names in [argNames] if [named],
     * or if a parameter with a default value precedes them.
     */
    private fun arguments(
        params: List<Param>,
        printsDefault: (Param) -> Boolean,
        argNames: List<String>,
        named: Boolean,
        typeArg: TypeRef? = null,
    ): Pair<List<String>, Boolean> {
        val passed = params.withIndex().filterNot { printsDefault(it.value) }
        val positional = !named && passed.withIndex().all { [index, param] -> param.index == index }
        val args = passed.map { [index, param] ->
            val value = value(substitute(param.type, typeArg), 0)
            if (positional) value else "${argNames.getOrNull(index) ?: param.name} = $value"
        }
        return args to positional
    }

    /** Substitutes the type parameter with [typeArg]. */
    private fun substitute(type: TypeRef, typeArg: TypeRef?): TypeRef =
        if (type is TypeRef.TypeParamType && typeArg != null) typeArg.withNullable(type.nullable || typeArg.nullable) else type

    private fun rootOverridden(memberId: Int): MemberFun? {
        val visited = mutableSetOf<Int>()
        var current = program.member(memberId)?.second ?: return null
        while (current.overrides != null && visited.add(current.id)) {
            current = program.member(current.overrides)?.second ?: break
        }
        return current
    }

    private fun StringBuilder.printBody(
        uses: List<Use>,
        returnType: TypeRef,
        variant: Int,
        indent: String = "    ",
        extraStatements: List<String> = emptyList(),
        implicitResult: Boolean = false,
    ) {
        for (use in uses) {
            appendLine("${indent}println(${useExpression(use)})")
        }
        for (statement in extraStatements) {
            appendLine("$indent$statement")
        }
        appendLine("$indent${if (implicitResult) "" else "return "}${value(returnType, variant)}")
    }

    private fun useExpression(use: Use): String = when (use) {
        is Use.CallFun -> {
            val decl = program.decl(use.funId) as? FunDecl
            if (decl == null) {
                unresolved(use.funId)
            } else {
                val [args, positional] = arguments(decl.params, decl::printsDefault, use.argNames, use.named, use.typeArg)
                val lambda = decl.lambda?.let { "{ ${use.lambdaVariant} }" }
                val receiver = decl.receiverClassId?.let { "${instance(it)}." } ?: ""
                val name = reference(decl).let { if (decl.typeParam != null) "$it<${type(use.typeArg)}>" else it }
                val binaryArg by lazy { value(substitute(decl.params.single().type, use.typeArg), 0) }
                // a call in the infix or the operator form infers the type argument from the argument
                val binary = decl.isBinary && (decl.typeParam == null || decl.params.single().type is TypeRef.TypeParamType)
                when {
                    binary && use.form == CallForm.INFIX -> "(${instance(decl.receiverClassId!!)} ${reference(decl)} $binaryArg)"
                    binary && use.form == CallForm.OPERATOR -> "(${instance(decl.receiverClassId!!)} + $binaryArg)"
                    lambda == null -> "$receiver$name(${args.joinToString()})"
                    positional -> "$receiver$name(${args.joinToString()}) $lambda"
                    else -> "$receiver$name(${(args + "$LAMBDA_PARAM = $lambda").joinToString()})"
                }
            }
        }
        is Use.ReadProp -> (program.decl(use.propId) as? PropDecl)?.let(::propertyRead) ?: unresolved(use.propId)
        is Use.Construct -> instance(use.classId)
        is Use.CallMember -> {
            val member = program.member(use.memberId)?.second?.let { member -> member.overrides?.let(::rootOverridden) ?: member }
            when {
                member == null -> "${instance(use.classId)}.${unresolved(use.memberId)}()"
                member.isProperty -> "${instance(use.classId)}.${member.name}"
                else -> "${instance(use.classId)}.${member.name}(${arguments(member.params, Param::hasDefault, use.argNames, use.named).first.joinToString()})"
            }
        }
        is Use.WhenSealed -> {
            val branches = use.branchClassIds.joinToString("; ") { "is ${classReference(it)} -> \"${it}\"" }
            "when (val v: ${classReference(use.sealedId)} = ${instance(use.scrutineeClassId)}) { $branches }"
        }
        is Use.WhenEnum -> {
            val enumName = classReference(use.enumId)
            val branches = use.entries.joinToString("; ") { "$enumName.$it -> \"$it\"" }
            "when ($enumName.${if (use.viaValues) "values()" else "entries"}.first()) { $branches }"
        }
        is Use.SmartCast -> {
            val prop = program.decl(use.propId) as? PropDecl
            if (prop == null) unresolved(use.propId) else smartCast(propertyRead(prop), prop.type)
        }
        is Use.SmartCastMember -> {
            val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
            if (member == null) unresolved(use.memberId) else smartCast("${instance(use.objectId)}.${member.name}", member.returnType)
        }
        is Use.WhenMember -> {
            val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
            if (member == null) unresolved(use.memberId) else "when (${value(member.returnType, 0)}) { ${instance(use.objectId)}.${member.name} -> \"c\"; else -> \"n\" }"
        }
        is Use.WhenConst -> {
            val prop = program.decl(use.propId) as? PropDecl
            if (prop == null) unresolved(use.propId) else "when (${value(prop.type, 0)}) { ${propertyRead(prop)} -> \"c\"; else -> \"n\" }"
        }
        is Use.ContractSmartCast -> {
            val decl = program.decl(use.funId) as? FunDecl
            val first = decl?.params?.firstOrNull()
            if (decl == null || first == null) {
                unresolved(use.funId)
            } else {
                val args = listOf("${first.name} = x") +
                        decl.params.drop(1).filterNot(decl::printsDefault).map { "${it.name} = ${value(it.type, 0)}" } +
                        listOfNotNull(decl.lambda?.let { "$LAMBDA_PARAM = { 0 }" })
                val receiver = decl.receiverClassId?.let { "${instance(it)}." } ?: ""
                "run { val x: ${type(first.type)} = null; $receiver${reference(decl)}(${args.joinToString()}); val y: ${type(first.type.withNullable(false))} = x; y }"
            }
        }
        is Use.SamConversion -> {
            val cls = program.classDecl(use.interfaceId)
            val sam = cls?.samMember
            when {
                cls == null -> unresolved(use.interfaceId)
                sam == null -> "${classReference(cls.id)} { 0 }"
                else -> {
                    val params = sam.params.joinToString { "_" }.let { if (it.isEmpty()) "" else "$it -> " }
                    "${classReference(cls.id)} { $params${value(sam.returnType, 0)} }"
                }
            }
        }
    }

    private fun type(type: TypeRef): String {
        val base = when (type) {
            is TypeRef.BuiltinType -> type.builtin.kotlinName
            is TypeRef.ClassType -> classReference(type.classId)
            is TypeRef.AliasType -> program.decl(type.aliasId)?.let(::reference) ?: unresolved(type.aliasId)
            is TypeRef.TypeParamType -> "T"
        }
        return if (type.nullable) "$base?" else base
    }

    private fun value(type: TypeRef, variant: Int, depth: Int = 0): String {
        if (depth > MAX_ALIAS_DEPTH) return "TODO()"
        return when (type) {
            is TypeRef.BuiltinType -> type.builtin.literal(variant)
            is TypeRef.ClassType -> {
                val cls = program.classDecl(type.classId)
                when {
                    cls == null -> unresolved(type.classId)
                    cls.isInstantiable -> instance(type.classId)
                    type.nullable -> "null"
                    else -> "TODO()"
                }
            }
            is TypeRef.AliasType -> {
                val alias = program.decl(type.aliasId) as? TypeAliasDecl
                if (alias == null) unresolved(type.aliasId) else value(alias.target, variant, depth + 1)
            }
            // a value of the type parameter is never needed: a function returns no value of it, and its parameters have no default values
            is TypeRef.TypeParamType -> "TODO()"
        }
    }

    private fun instance(classId: Int): String {
        val cls = program.classDecl(classId) ?: return unresolved(classId)
        if (isInner(cls)) return "${instance(outerOf(cls)!!.id)}.${cls.name}()"
        val name = reference(cls)
        return when (cls.kind) {
            ClassKind.OBJECT -> name
            ClassKind.ENUM -> "$name.entries.first()"
            ClassKind.ABSTRACT, ClassKind.SEALED, ClassKind.INTERFACE, ClassKind.SEALED_INTERFACE -> "(TODO() as $name)"
            ClassKind.FINAL, ClassKind.OPEN, ClassKind.DATA -> "$name()"
            ClassKind.VALUE -> "$name(0)"
        }
    }

    private fun classReference(classId: Int): String = program.decl(classId)?.let(::reference) ?: unresolved(classId)

    /**
     * Returns the name of the top-level [decl] and imports it if it is declared in another package, explicitly, with an alias, or with a star.
     * A nested class is referenced through its outer class.
     */
    private fun reference(decl: Decl): String {
        if (decl is ClassDecl) outerOf(decl)?.let { return "${reference(it)}.${decl.name}" }
        val file = program.fileOf(decl.id) ?: return decl.name
        if (file.pkg == currentFile.pkg) return decl.name
        return when {
            decl.id in currentFile.aliasedIds -> "${decl.name}$ALIAS_SUFFIX".also { imports += "${file.pkg}.${decl.name} as $it" }
            currentFile.starImports -> decl.name.also { imports += "${file.pkg}.*" }
            else -> decl.name.also { imports += "${file.pkg}.${decl.name}" }
        }
    }

    private fun unresolved(id: Int) = "unresolved$id"

    companion object {
        private const val MAX_ALIAS_DEPTH = 8
        private const val LAMBDA_PARAM = "block"
        private const val ALIAS_SUFFIX = "Alias"

        /** The kinds of classes which may be inner. */
        private val INNER_KINDS = setOf(ClassKind.FINAL, ClassKind.OPEN, ClassKind.ABSTRACT)

        fun print(program: Program): Map<String, String> {
            val printer = ProgramPrinter(program)
            return program.files.associate { it.name to printer.printFile(it) }
        }
    }
}
