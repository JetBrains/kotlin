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
        is TypeAliasDecl -> "${decl.visibility.keyword}typealias ${decl.name}${if (decl.generic) "<T>" else ""} = ${type(decl.target)}\n"
        is ClassDecl -> printClass(decl)
    }

    private fun printFun(decl: FunDecl): String = buildString {
        append(annotations(decl.annotations))
        if (decl.optInIds.isNotEmpty()) appendLine("@OptIn(${decl.optInIds.joinToString { "${classReference(it)}::class" }})")
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
        decl.contextClassId?.let { append("context(ctx: ${typeReference(it)}) ") }
        append(decl.visibility.keyword)
        if (decl.inline) append("inline ")
        if (decl.infix && decl.isBinary) append("infix ")
        if (decl.isOperator) append("operator ")
        if (decl.isSuspend) append("suspend ")
        append("fun ")
        decl.typeParam?.let { append("<${if (decl.isReified) "reified " else ""}T${if (decl.typeParamNonNull) " : Any" else ""}> ") }
        decl.receiverClassId?.let { append("${typeReference(it)}.") }
        append("${decl.name}(")
        // a getValue operator has the parameters of the convention instead of the ones of the model
        val params = if (decl.isOperator && decl.operatorKind == OperatorKind.GET_VALUE) {
            listOf("thisRef: Any?", "property: kotlin.reflect.KProperty<*>")
        } else {
            parameters(decl.params, decl::printsDefault)
        }
        val lambdaParam = decl.lambda?.let { kind ->
            val modifier = when {
                !decl.inline -> ""
                kind == LambdaKind.NOINLINE && KnownIssues.inlineOfSeparateCompilation -> "noinline "
                kind == LambdaKind.CROSSINLINE -> "crossinline "
                else -> ""
            }
            "$modifier$LAMBDA_PARAM: ${decl.printedLambdaShape.type}"
        }
        append((params + listOfNotNull(lambdaParam)).joinToString())
        // an inferred return type is the type of the last expression of the `run` lambda, the statements are kept;
        // a contract has to be the first statement of a block body; the code of an inline function is inlined through the `run` lambda,
        // see KnownIssues.inlineOfSeparateCompilation
        val implicitReturnType = decl.implicitReturnType && !decl.hasContract && isInferable(decl.returnType, decl.bodyVariant) &&
                (!decl.inline || KnownIssues.inlineOfSeparateCompilation)
        appendLine(if (implicitReturnType) ") = run {" else "): ${type(decl.returnType)} {")
        if (decl.hasContract) appendLine("    kotlin.contracts.contract { returns() implies (${decl.params.first().name} != null) }")
        val extraStatements = buildList {
            val invocation = lambdaInvocation(decl)
            when {
                invocation == null -> {}
                // a crossinline lambda is called from an object, which is a class owned by the inline function copied to the call sites
                decl.inline && decl.lambda == LambdaKind.CROSSINLINE && KnownIssues.inlineFunOwnedClass -> {
                    if (decl.printedLambdaShape != LambdaShape.SUSPEND) add("object : Runnable { override fun run() { println($invocation) } }.run()")
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
            // local declarations are not allowed in inline functions; the local classes of the overloads have the same JVM names
            // unless their names differ
            if (!decl.inline && decl.localClassVariant != 0) {
                add("class L${decl.id} { override fun toString() = \"l${decl.localClassVariant}\" }")
                add("println(L${decl.id}())")
            }
            if (!decl.inline && decl.localFunVariant != 0) {
                add("fun lf${decl.id}() = \"f${decl.localFunVariant}\"")
                add("println(lf${decl.id}())")
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
        val receiver = decl.printedReceiverId?.let { "${typeReference(it)}." } ?: ""
        val accessor = when {
            decl.kind == PropKind.CONST -> PropAccessor.FIELD
            decl.printedReceiverId != null && decl.accessor != PropAccessor.GETTER && decl.accessor != PropAccessor.INLINE_GETTER -> PropAccessor.GETTER
            decl.accessor == PropAccessor.LAZY && decl.kind != PropKind.VAL -> PropAccessor.FIELD
            // a delegate needs the getValue operator, and a setValue one for a var
            decl.accessor == PropAccessor.DELEGATE && (decl.kind != PropKind.VAL || decl.printedReceiverId != null || delegateOf(decl) == null) ->
                PropAccessor.FIELD
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
        val declaration = "${annotations(decl.annotations)}$annotation${decl.visibility.keyword}$inline$keyword $receiver${decl.name}$typeAnnotation"
        return when {
            lateinit -> "$declaration\n"
            accessor == PropAccessor.FIELD -> "$declaration = $value\n"
            accessor == PropAccessor.LAZY -> "$declaration by lazy { $value }\n"
            accessor == PropAccessor.DELEGATE -> "$declaration by ${instance(delegateOf(decl)!!.receiverClassId!!)}\n"
            else -> "$declaration\n    get() = $value\n" + if (isVar) "    set(value) {}\n" else ""
        }
    }

    private fun isInlineProperty(propId: Int): Boolean =
        (program.decl(propId) as? PropDecl)?.let { it.accessor == PropAccessor.INLINE_GETTER && it.kind != PropKind.CONST } == true

    /** A `lateinit` property has a non-nullable type which is not primitive. */
    private fun allowsLateinit(type: TypeRef): Boolean = !type.nullable && when (type) {
        is TypeRef.BuiltinType -> type.builtin == Builtin.STRING || type.builtin == Builtin.ANY
        is TypeRef.ClassType -> program.classDecl(type.classId)?.kind != ClassKind.VALUE
        is TypeRef.ListType -> true
        is TypeRef.AliasType, is TypeRef.TypeParamType -> false
    }

    /** Whether [type] is a value class type, possibly through typealiases. */
    private fun isValueClassType(type: TypeRef, depth: Int = 0): Boolean = depth <= MAX_ALIAS_DEPTH && when (type) {
        is TypeRef.ClassType -> program.classDecl(type.classId)?.kind == ClassKind.VALUE
        is TypeRef.AliasType -> (program.decl(type.aliasId) as? TypeAliasDecl)?.let { isValueClassType(it.target, depth + 1) } == true
        is TypeRef.BuiltinType, is TypeRef.TypeParamType, is TypeRef.ListType -> false
    }

    /** A read of [prop], on an instance of its receiver for an extension property. */
    private fun propertyRead(prop: PropDecl): String = (prop.printedReceiverId?.let { "${instance(it)}." } ?: "") + reference(prop)

    /** Whether the value of [type] has a type which may be inferred: `TODO()` and `null` would be inferred as `Nothing` and `Nothing?`. */
    private fun isInferable(type: TypeRef, variant: Int): Boolean = value(type, variant).let { it != "TODO()" && it != "null" }

    private fun printClass(decl: ClassDecl): String = buildString {
        append(annotations(decl.annotations))
        if (decl.kind == ClassKind.ANNOTATION) {
            appendLine("@Retention(AnnotationRetention.${decl.retention.name})")
            when (decl.annotationTargets) {
                AnnotationTargets.ALL -> {}
                AnnotationTargets.CLASSES -> appendLine("@Target(AnnotationTarget.CLASS)")
                AnnotationTargets.FUNCTIONS -> appendLine("@Target(AnnotationTarget.FUNCTION)")
            }
            if (decl.requiresOptIn) appendLine("@RequiresOptIn")
            appendLine("${decl.visibility.keyword}annotation class ${decl.name}(val v: String = \"\")")
            return@buildString
        }
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
                ClassKind.INTERFACE -> if (decl.isFunInterface) "fun interface " else "interface "
                ClassKind.SEALED_INTERFACE -> "sealed interface "
                ClassKind.DATA -> "data class "
                ClassKind.OBJECT -> "object "
                ClassKind.ENUM -> "enum class "
                ClassKind.VALUE -> "value class "
                ClassKind.ANNOTATION -> error("printed above")
            }
        )
        append(decl.name)
        if (decl.isGeneric) append("<${decl.typeParam!!.keyword}T${if (decl.typeParamNonNull) " : Any" else ""}>")
        val ctorParams = decl.printedCtorParams.map { param ->
            "${if (param.isProperty) "val " else ""}${param.name}: ${type(param.type)}" +
                    if (param.hasDefault) " = ${value(param.type, param.defaultVariant)}" else ""
        }
        val ctorVisibility = if (decl.kind.hasConstructor && decl.ctorVisibility != Visibility.PUBLIC) " ${decl.ctorVisibility.keyword}constructor" else ""
        when {
            decl.kind == ClassKind.DATA -> append("$ctorVisibility(${(listOf("val v: Int = 0") + ctorParams).joinToString()})")
            decl.kind == ClassKind.VALUE -> append("(val v: ${if (decl.isGeneric) "T" else "Int"})")
            decl.kind == ClassKind.ENUM && decl.enumConstructorVariant != 0 -> append("(val v: Int)")
            // the secondary constructor delegates to an explicit primary one
            ctorParams.isNotEmpty() || ctorVisibility.isNotEmpty() || decl.secondaryCtor && decl.kind.hasConstructor ->
                append("$ctorVisibility(${ctorParams.joinToString()})")
        }
        decl.superId?.let { superId ->
            val superClass = program.classDecl(superId)
            append(" : ")
            append(typeReference(superId))
            if (superClass != null && !superClass.kind.isInterface && !decl.kind.isInterface) append("(${constructorArguments(superClass)})")
        }
        if (decl.interfaceIds.isNotEmpty()) {
            append(if (decl.superId == null) " : " else ", ")
            append(decl.interfaceIds.joinToString { typeReference(it) + if (it in decl.printedDelegatedIds) " by d$it" else "" })
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
        if (decl.initVariant != 0 && !decl.kind.isInterface) appendLine("    init { println(\"i${decl.initVariant}\") }")
        // the `Unit` parameter keeps the signature different from the primary constructor
        // in the class, its type parameter is not substituted
        if (decl.secondaryCtor && decl.kind.hasConstructor) appendLine("    constructor(s: String, u: Unit) : this(${constructorArguments(decl, typeArg = null)})")
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
        cls.nesting == Nesting.INNER && cls.kind in INNER_KINDS && outerOf(cls)?.let { it.kind.isRegularClass && !it.isGeneric } == true

    /** A companion object is printed as a nested object where a companion object is not allowed, or if the class already has one. */
    private fun isCompanion(cls: ClassDecl): Boolean {
        val outer = outerOf(cls) ?: return false
        if (cls.nesting != Nesting.COMPANION || cls.kind != ClassKind.OBJECT) return false
        if (!outer.kind.isRegularClass && !outer.kind.isInterface && outer.kind != ClassKind.ENUM) return false
        return program.classes.first { outerOf(it) == outer && it.nesting == Nesting.COMPANION && it.kind == ClassKind.OBJECT } == cls
    }

    private fun StringBuilder.printMember(owner: ClassDecl, member: MemberFun) {
        // an override in a subclass, which passes `Int` as the type argument, has `Int` instead of the type parameter of the class
        val root = member.overrides?.let(::rootOverridden) ?: member
        val rootOwner = program.member(root.id)?.first
        val signatureSource = if (rootOwner != null && rootOwner.id != owner.id) {
            root.copy(returnType = substitute(root.returnType, INT_TYPE), params = root.params.map { it.copy(type = substitute(it.type, INT_TYPE)) })
        } else {
            root
        }
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
        // the properties of the companion of an interface may be @JvmField only all together
        val jvmField = member.jvmField && accessor == PropAccessor.FIELD && member.overrides == null && member.modality == Modality.FINAL &&
                !isConst && !isLateinit && !isValueClassType(returnType) && !(isCompanion(owner) && outerOf(owner)?.kind?.isInterface == true)
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
        when (decl.printedLambdaShape) {
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

    /** Substitutes the type parameter with [typeArg] in [type]. */
    private fun substitute(type: TypeRef, typeArg: TypeRef?): TypeRef = if (typeArg == null) type else when (type) {
        is TypeRef.TypeParamType -> typeArg.withNullable(type.nullable || typeArg.nullable)
        is TypeRef.ListType -> type.copy(element = substitute(type.element, typeArg))
        is TypeRef.ClassType -> type.copy(typeArg = type.typeArg?.let { substitute(it, typeArg) })
        is TypeRef.AliasType -> type.copy(typeArg = type.typeArg?.let { substitute(it, typeArg) })
        is TypeRef.BuiltinType -> type
    }

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
        is Use.CallFun -> callFun(use).let { call -> use.scopeClassId?.let { "with(${instance(it)}) { $call }" } ?: call }
        is Use.ReadProp -> (program.decl(use.propId) as? PropDecl)?.let(::propertyRead) ?: unresolved(use.propId)
        is Use.Construct -> {
            val cls = program.classDecl(use.classId)
            if (use.viaSecondary && cls != null && cls.secondaryCtor && cls.kind.hasConstructor && !isInner(cls)) {
                "${typeReference(cls.id)}(s = \"x\", u = Unit)"
            }
            else instance(use.classId)
        }
        is Use.Destructure -> {
            val instance = instance(use.classId)
            if (use.propertyName != null) {
                "run { val (${use.propertyName}) = $instance; ${use.propertyName} }"
            } else {
                val names = (1..use.componentCount).map { "c$it" }
                "run { val [${names.joinToString()}] = $instance; c1 }"
            }
        }
        is Use.Copy -> "${instance(use.classId)}.copy()"
        is Use.ReadCtorProperty -> "${instance(use.classId)}.${use.name}"
        is Use.CallMember -> {
            val member = program.member(use.memberId)?.second?.let { member -> member.overrides?.let(::rootOverridden) ?: member }
            val call = when {
                member == null -> "${unresolved(use.memberId)}()"
                member.isProperty -> member.name
                else -> "${member.name}(${arguments(member.params, Param::hasDefault, use.argNames, use.named, INT_TYPE).first.joinToString()})"
            }
            // an implicit call may resolve to another function with the same name
            if (use.implicit) "with(${instance(use.classId)}) { $call }" else "${instance(use.classId)}.$call"
        }
        is Use.WhenSealed -> {
            // a generic class is checked with a star projection, as its type argument is erased
            val branches = use.branchClassIds.joinToString("; ") { "is ${typeReference(it, star = true)} -> \"${it}\"" }
            "when (val v: ${typeReference(use.sealedId, star = true)} = ${instance(use.scrutineeClassId)}) { $branches }"
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
            val sam = cls?.takeIf { it.isFunInterface }?.samMember
            when {
                cls == null -> unresolved(use.interfaceId)
                sam == null -> "${typeReference(cls.id)} { 0 }"
                else -> {
                    val params = sam.params.joinToString { "_" }.let { if (it.isEmpty()) "" else "$it -> " }
                    "${typeReference(cls.id)} { $params${value(substitute(sam.returnType, INT_TYPE), 0)} }"
                }
            }
        }
    }

    /** A call of a function, in the infix or the operator form of the call if the function has the modifier. */
    private fun callFun(use: Use.CallFun): String {
        val decl = program.decl(use.funId) as? FunDecl ?: return unresolved(use.funId)
        val [args, positional] = arguments(decl.params, decl::printsDefault, use.argNames, use.named, use.typeArg)
        val lambda = decl.lambda?.let { "{ ${use.lambdaVariant} }" }
        val receiver = decl.receiverClassId?.let { "${instance(it)}." } ?: ""
        val name = reference(decl).let { if (decl.typeParam != null) "$it<${type(use.typeArg)}>" else it }
        val binaryArg by lazy { value(substitute(decl.params.single().type, use.typeArg), 0) }
        // a call in the infix or the operator form infers the type argument from the argument
        val binary = decl.isBinary && (decl.typeParam == null || decl.params.single().type is TypeRef.TypeParamType)
        val operand by lazy { instance(decl.receiverClassId!!) }
        val regularArgs = when {
            lambda == null -> "(${args.joinToString()})"
            positional -> "(${args.joinToString()}) $lambda"
            else -> "(${(args + "$LAMBDA_PARAM = $lambda").joinToString()})"
        }
        return when {
            binary && use.form == CallForm.INFIX -> "($operand ${reference(decl)} $binaryArg)"
            decl.isOperator && decl.operatorKind == OperatorKind.GET_VALUE -> "$operand.${reference(decl)}(null, String::length)"
            decl.isOperator && use.form == CallForm.OPERATOR && (decl.typeParam == null || binary) -> when (decl.operatorKind) {
                OperatorKind.PLUS -> "($operand + $binaryArg)"
                OperatorKind.GET -> "$operand[$binaryArg]"
                OperatorKind.COMPARE_TO -> "($operand < $binaryArg)"
                OperatorKind.INVOKE -> "$operand$regularArgs"
                OperatorKind.COMPONENT1 -> "run { val [c] = $operand; c }"
                OperatorKind.GET_VALUE -> error("handled above")
            }
            else -> "$receiver$name$regularArgs"
        }
    }

    /** The getValue operator a delegated property delegates to, `null` if it is not an operator any more. */
    private fun delegateOf(prop: PropDecl): FunDecl? =
        (prop.delegateFunId?.let(program::decl) as? FunDecl)?.takeIf { it.isOperator && it.operatorKind == OperatorKind.GET_VALUE }

    private fun type(type: TypeRef): String {
        val base = when (type) {
            is TypeRef.BuiltinType -> type.builtin.kotlinName
            is TypeRef.ClassType -> typeReference(type.classId, type.typeArg, type.star)
            is TypeRef.AliasType -> {
                val alias = program.decl(type.aliasId) as? TypeAliasDecl
                when {
                    alias == null -> program.decl(type.aliasId)?.let(::reference) ?: unresolved(type.aliasId)
                    alias.generic -> "${reference(alias)}<${type(type.typeArg ?: INT_TYPE)}>"
                    else -> reference(alias)
                }
            }
            is TypeRef.TypeParamType -> "T"
            is TypeRef.ListType -> "List<${type(type.element)}>"
        }
        return if (type.nullable) "$base?" else base
    }

    /** A reference to the class [classId] as a type, with the type argument of a generic class: [typeArg], `Int` by default, or a star. */
    private fun typeReference(classId: Int, typeArg: TypeRef? = null, star: Boolean = false): String {
        val reference = classReference(classId)
        val cls = program.classDecl(classId)
        if (cls == null || !cls.isGeneric) return reference
        return "$reference<${if (star) "*" else type(typeArg ?: INT_TYPE)}>"
    }

    private fun value(type: TypeRef, variant: Int, depth: Int = 0): String {
        if (depth > MAX_ALIAS_DEPTH) return "TODO()"
        return when (type) {
            is TypeRef.BuiltinType -> type.builtin.literal(variant)
            is TypeRef.ClassType -> {
                val cls = program.classDecl(type.classId)
                when {
                    cls == null -> unresolved(type.classId)
                    cls.isInstantiable -> instance(type.classId, type.typeArg.takeUnless { type.star })
                    type.nullable -> "null"
                    else -> "TODO()"
                }
            }
            is TypeRef.AliasType -> {
                val alias = program.decl(type.aliasId) as? TypeAliasDecl
                if (alias == null) unresolved(type.aliasId)
                else value(if (alias.generic) substitute(alias.target, type.typeArg ?: INT_TYPE) else alias.target, variant, depth + 1)
            }
            // the value of a type parameter is only returned, and a parameter of it has no default value
            is TypeRef.TypeParamType -> "TODO()"
            is TypeRef.ListType -> "listOf(${value(type.element, variant, depth + 1)})"
        }
    }

    /** An instance of the class [classId], with the type argument [typeArg] of a generic class, `Int` by default. */
    private fun instance(classId: Int, typeArg: TypeRef? = null): String {
        val cls = program.classDecl(classId) ?: return unresolved(classId)
        // a constructor parameter may have the type of its class
        if (instanceDepth > MAX_INSTANCE_DEPTH) return "TODO()"
        instanceDepth++
        try {
            val arg = typeArg ?: INT_TYPE
            val typeArgs = if (cls.isGeneric) "<${type(arg)}>" else ""
            if (isInner(cls)) return "${instance(outerOf(cls)!!.id)}.${cls.name}$typeArgs(${constructorArguments(cls, arg)})"
            val name = reference(cls) + typeArgs
            return when (cls.kind) {
                ClassKind.OBJECT -> reference(cls)
                ClassKind.ENUM -> "${reference(cls)}.entries.first()"
                ClassKind.ABSTRACT, ClassKind.SEALED, ClassKind.INTERFACE, ClassKind.SEALED_INTERFACE, ClassKind.ANNOTATION -> "(TODO() as $name)"
                ClassKind.FINAL, ClassKind.OPEN, ClassKind.DATA -> "$name(${constructorArguments(cls, arg)})"
                ClassKind.VALUE -> "$name(${if (cls.isGeneric) value(arg, 0) else "0"})"
            }
        } finally {
            instanceDepth--
        }
    }

    private var instanceDepth = 0

    /** The arguments of a call of the primary constructor of [cls], passing the parameters without default values by name. */
    private fun constructorArguments(cls: ClassDecl, typeArg: TypeRef? = INT_TYPE): String =
        cls.printedCtorParams.filterNot { it.hasDefault }.joinToString { "${it.name} = ${value(substitute(it.type, typeArg), 0)}" }

    private fun annotations(uses: List<AnnotationUse>): String = uses.joinToString("") { use ->
        "@${classReference(use.annotationId)}(\"a${use.argVariant}\")\n"
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
        private const val MAX_INSTANCE_DEPTH = 4
        private val INT_TYPE: TypeRef = TypeRef.BuiltinType(Builtin.INT)
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
