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
        // a nested class or typealias is printed in its outer class
        val decls = file.decls.filterNot { it is ClassDecl && outerOf(it) != null || it is TypeAliasDecl && outerOf(it) != null }
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
        nonNullTypeParams = buildSet {
            if (decl.typeParamNonNull) add(0)
            if (decl.typeParam2 && decl.whereClause && decl.typeParamNonNull) add(1)
        }
        decl.stdlibShadow?.let { shadow ->
            append(
                when (shadow) {
                    StdlibShadow.PRINTLN -> "fun println(message: Any?) {\n    kotlin.io.println(message)\n}\n"
                    StdlibShadow.LIST_OF -> "fun <T> listOf(element: T): List<T> = kotlin.collections.listOf(element)\n"
                }
            )
            return@buildString
        }
        append(annotations(decl.annotations))
        // a declaration has a single @OptIn with all its markers
        val jvmExposeBoxed = decl.jvmExposeBoxed && !decl.isSuspend && !decl.isReified && decl.visibility != Visibility.PRIVATE && !decl.jvmSynthetic &&
                (!decl.inline || KnownIssues.jvmExposeBoxedInline)
        val optIns = decl.optInIds.map { "${classReference(it)}::class" } +
                listOfNotNull("kotlin.contracts.ExperimentalContracts::class".takeIf { decl.hasContract }, "ExperimentalStdlibApi::class".takeIf { jvmExposeBoxed })
        if (optIns.isNotEmpty()) appendLine("@OptIn(${optIns.joinToString()})")
        // the boxed variant would clash with a declaration without a value class parameter or with a @JvmName, so it has its own name
        if (jvmExposeBoxed) appendLine("@JvmExposeBoxed(\"e${decl.id}\")")
        if (decl.deprecationConstId != null || decl.hidden || decl.deprecationError) {
            val message = decl.deprecationConstId?.let { constId -> "\${${program.decl(constId)?.let(::reference) ?: unresolved(constId)}}" } ?: "h"
            val replaceWith = if (decl.replaceWith) ", ReplaceWith(\"x\")" else ""
            val level = when {
                decl.hidden -> ", level = DeprecationLevel.HIDDEN"
                decl.deprecationError -> ", level = DeprecationLevel.ERROR"
                else -> ""
            }
            appendLine("@Deprecated(\"$message\"$replaceWith$level)")
        }
        // the overloads have the same name, so the JVM name is unique only with the id
        if (decl.jvmNameVariant != 0) appendLine("@JvmName(\"j${decl.id}v${decl.jvmNameVariant}\")")
        if (decl.jvmOverloads && !decl.inline && decl.params.any(decl::printsDefault)) appendLine("@JvmOverloads")
        if (decl.publishedApi && decl.visibility == Visibility.INTERNAL) appendLine("@PublishedApi")
        if (decl.jvmSynthetic) appendLine("@JvmSynthetic")
        if (decl.throws) appendLine("@Throws(IllegalStateException::class)")
        if (decl.synchronized && !decl.isSuspend) appendLine("@Synchronized")
        if (decl.jvmSuppressWildcards) appendLine("@JvmSuppressWildcards")
        decl.contextClassId?.let { append("context(ctx: ${typeReference(it)}) ") }
        append(decl.visibility.keyword)
        // an external function has no body, so neither a contract nor the code of an inline or a suspend function
        val external = decl.external && !decl.inline && !decl.isSuspend && !decl.hasContract && !decl.isOperator && !jvmExposeBoxed
        if (external) append("external ")
        // a tailrec function without a tail call is only a warning
        if (decl.tailrec && !decl.inline && !decl.isSuspend && !external) append("tailrec ")
        if (decl.inline) append("inline ")
        if (decl.infix && decl.isBinary) append("infix ")
        if (decl.isOperator) append("operator ")
        if (decl.isSuspend) append("suspend ")
        append("fun ")
        decl.typeParam?.let {
            val second = if (decl.typeParam2) ", U" else ""
            append("<${if (decl.isReified) "reified " else ""}T${if (decl.typeParamNonNull) " : Any" else ""}$second> ")
        }
        decl.receiverClassId?.let { append("${typeReference(it)}.") }
        append("${decl.name}(")
        // the delegation operators have the parameters of the convention instead of the ones of the model,
        // the setValue one sets a value of its return type and returns Unit, the provideDelegate one provides a property of it
        val isSetValue = decl.isOperator && decl.operatorKind == OperatorKind.SET_VALUE
        val isProvideDelegate = decl.isOperator && decl.operatorKind == OperatorKind.PROVIDE_DELEGATE
        val params = when {
            decl.isOperator && decl.operatorKind == OperatorKind.GET_VALUE || isProvideDelegate -> listOf("thisRef: Any?", "property: kotlin.reflect.KProperty<*>")
            isSetValue -> listOf("thisRef: Any?", "property: kotlin.reflect.KProperty<*>", "value: ${type(decl.returnType)}")
            else -> parameters(if (decl.inline) decl.params.map(::inlineFunctionParam) else decl.params, decl::printsDefault, decl.inline)
        }
        val returnType = if (isSetValue) UNIT_TYPE else decl.returnType
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
        val implicitReturnType = decl.implicitReturnType && !decl.hasContract && !isSetValue && !isProvideDelegate && !external &&
                isInferable(decl.returnType, decl.bodyVariant) && (!decl.inline || KnownIssues.inlineOfSeparateCompilation)
        val whereClause = if (decl.typeParam != null && decl.typeParam2 && decl.whereClause) " where U : T" else ""
        val printedReturnType = if (isProvideDelegate) "kotlin.properties.ReadOnlyProperty<Any?, ${type(returnType)}>" else type(returnType)
        if (external) {
            appendLine("): $printedReturnType$whereClause")
            return@buildString
        }
        appendLine(if (implicitReturnType) ")$whereClause = run {" else "): $printedReturnType$whereClause {")
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
        val uses = if (decl.inline) withoutNestedInlining(decl.uses) else decl.uses
        val result = if (isProvideDelegate) "kotlin.properties.ReadOnlyProperty { _, _ -> ${value(returnType, decl.bodyVariant)} }" else null
        inlineBody = decl.inline
        printBody(uses, returnType, decl.bodyVariant, extraStatements = extraStatements, implicitResult = implicitReturnType, result = result)
        inlineBody = false
        appendLine("}")
    }

    /** Whether the body being printed is the one of an inline function, whose classes are copied to its call sites. */
    private var inlineBody = false

    private fun printProp(decl: PropDecl): String {
        val receiver = decl.printedReceiverId?.let { "${typeReference(it)}." } ?: ""
        val accessor = when {
            decl.kind == PropKind.CONST -> PropAccessor.FIELD
            decl.printedReceiverId != null && decl.accessor != PropAccessor.GETTER && decl.accessor != PropAccessor.INLINE_GETTER -> PropAccessor.GETTER
            decl.accessor == PropAccessor.LAZY && decl.kind != PropKind.VAL -> PropAccessor.FIELD
            // a delegate needs the getValue operator, and a setValue one with the same receiver for a var,
            // the property provided by a provideDelegate operator is read-only
            decl.accessor == PropAccessor.DELEGATE &&
                    (decl.printedReceiverId != null || delegateOf(decl) == null ||
                            decl.kind != PropKind.VAL && (setterDelegateOf(decl) == null || delegateOf(decl)?.operatorKind == OperatorKind.PROVIDE_DELEGATE)) ->
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
        // the accessors of a constant and of a field are not renamed
        val accessorJvmNames = decl.accessorJvmNameVariant != 0 && decl.kind != PropKind.CONST && !jvmField
        val annotation = (if (jvmField) "@JvmField " else "") + (if (volatile) "@Volatile " else "") + if (accessorJvmNames) {
            "@get:JvmName(\"g${decl.id}v${decl.accessorJvmNameVariant}\") " + if (isVar) "@set:JvmName(\"s${decl.id}v${decl.accessorJvmNameVariant}\") " else ""
        } else ""
        val inline = if (accessor == PropAccessor.INLINE_GETTER) "inline " else ""
        val typeAnnotation = if (!lateinit && decl.implicitType && isInferable(decl.type, decl.valueVariant)) "" else ": ${type(decl.type)}"
        val value = value(decl.type, decl.valueVariant)
        val declaration = "${annotations(decl.annotations)}$annotation${decl.visibility.keyword}$inline$keyword $receiver${decl.name}$typeAnnotation"
        val setter = if (isVar && accessor != PropAccessor.LAZY && accessor != PropAccessor.DELEGATE && !jvmField) {
            setter(decl.visibility, decl.setterVisibility, decl.setterBodyVariant, hasField = accessor == PropAccessor.FIELD, indent = "    ")
        } else null
        return when {
            // a lateinit property may have a private setter, but no setter body
            lateinit -> "$declaration\n" + (setter?.takeIf { decl.setterBodyVariant == 0 }?.let { "$it\n" } ?: "")
            accessor == PropAccessor.FIELD -> "$declaration = $value\n" + (setter?.let { "$it\n" } ?: "")
            accessor == PropAccessor.LAZY -> "$declaration by lazy { $value }\n"
            accessor == PropAccessor.DELEGATE -> "$declaration by ${instance(delegateOf(decl)!!.receiverClassId!!)}\n"
            else -> "$declaration\n    get() = $value\n" + if (isVar) "${setter ?: "    set(value) {}"}\n" else ""
        }
    }

    /**
     * The setter of a `var` with the [setterVisibility] if it is more restrictive than the [propertyVisibility], and a body printing
     * the [bodyVariant] if it is not 0, which stores the value in the field if the property [hasField].
     * Returns `null` if the setter is the default one.
     */
    private fun setter(propertyVisibility: Visibility, setterVisibility: Visibility?, bodyVariant: Int, hasField: Boolean, indent: String): String? {
        val visibility = setterVisibility?.takeIf { RESTRICTIVENESS.indexOf(it) > RESTRICTIVENESS.indexOf(propertyVisibility) }?.keyword ?: ""
        val body = when {
            bodyVariant != 0 && hasField -> "(value) { println(\"w$bodyVariant\"); field = value }"
            bodyVariant != 0 -> "(value) { println(\"w$bodyVariant\") }"
            !hasField -> "(value) {}"
            else -> ""
        }
        if (visibility.isEmpty() && body.isEmpty()) return null
        return "$indent${visibility}set$body"
    }

    private fun isInlineProperty(propId: Int): Boolean =
        (program.decl(propId) as? PropDecl)?.let { it.accessor == PropAccessor.INLINE_GETTER && it.kind != PropKind.CONST } == true

    /** A `lateinit` property has a non-nullable type which is not primitive. */
    private fun allowsLateinit(type: TypeRef): Boolean = !type.nullable && when (type) {
        is TypeRef.BuiltinType -> type.builtin == Builtin.STRING || type.builtin == Builtin.ANY || type.builtin == Builtin.INT_ARRAY
        is TypeRef.ClassType -> program.classDecl(type.classId)?.kind != ClassKind.VALUE
        is TypeRef.ListType, is TypeRef.FunctionType, is TypeRef.ArrayType -> true
        is TypeRef.AliasType, is TypeRef.TypeParamType -> false
    }

    /** Whether [type] is a value class type, possibly through typealiases. */
    private fun isValueClassType(type: TypeRef, depth: Int = 0): Boolean = depth <= MAX_ALIAS_DEPTH && when (type) {
        is TypeRef.ClassType -> program.classDecl(type.classId)?.kind == ClassKind.VALUE
        is TypeRef.AliasType -> (program.decl(type.aliasId) as? TypeAliasDecl)?.let { isValueClassType(it.target, depth + 1) } == true
        is TypeRef.BuiltinType, is TypeRef.TypeParamType, is TypeRef.ListType, is TypeRef.FunctionType, is TypeRef.ArrayType -> false
    }

    /** A read of [prop], on an instance of its receiver for an extension property. */
    private fun propertyRead(prop: PropDecl): String = (prop.printedReceiverId?.let { "${instance(it)}." } ?: "") + reference(prop)

    /**
     * Whether the type inferred from the value of [type] is [type] or its non-null version: `TODO()` and `null` would be inferred
     * as `Nothing` and `Nothing?`, and a lambda is never inferred as a suspend one, also through a typealias.
     */
    private fun isInferable(type: TypeRef, variant: Int): Boolean =
        value(type, variant).let { it != "TODO()" && it != "null" } && functionType(type)?.suspend != true

    private fun printClass(decl: ClassDecl): String = buildString {
        nonNullTypeParams = if (decl.isGeneric && decl.typeParamNonNull) setOf(0) else emptySet()
        append(annotations(decl.annotations))
        if (decl.jvmDefaultWithoutCompatibility) appendLine("@JvmDefaultWithoutCompatibility")
        if (decl.kind == ClassKind.ANNOTATION) {
            appendLine("@Retention(AnnotationRetention.${decl.retention.name})")
            when (decl.annotationTargets) {
                AnnotationTargets.ALL -> {}
                AnnotationTargets.CLASSES -> appendLine("@Target(AnnotationTarget.CLASS)")
                AnnotationTargets.FUNCTIONS -> appendLine("@Target(AnnotationTarget.FUNCTION)")
                AnnotationTargets.TYPES -> appendLine("@Target(AnnotationTarget.TYPE)")
            }
            if (decl.repeatable) appendLine("@Repeatable")
            if (decl.requiresOptIn) appendLine(if (decl.optInWarningLevel) "@RequiresOptIn(level = RequiresOptIn.Level.WARNING)" else "@RequiresOptIn")
            val params = buildList {
                add("val v: String = \"\"")
                if (AnnotationParam.KCLASS in decl.annotationParams) add("val k: kotlin.reflect.KClass<*> = Any::class")
                annotationEnumOf(decl)?.let { enum -> add("val e: ${classReference(enum.id)} = ${classReference(enum.id)}.${enum.enumEntries.first()}") }
                if (AnnotationParam.ARRAY in decl.annotationParams) add("val a: IntArray = []")
            }
            appendLine("${decl.visibility.keyword}annotation class ${decl.name}(${params.joinToString()})")
            return@buildString
        }
        if (decl.kind == ClassKind.VALUE) appendLine("@JvmInline")
        // a declaration has a single @OptIn with all its markers
        val jvmExposeBoxed = decl.kind == ClassKind.VALUE && decl.jvmExposeBoxed
        val optIns = decl.optInIds.map { "${classReference(it)}::class" } + listOfNotNull("ExperimentalStdlibApi::class".takeIf { jvmExposeBoxed })
        if (optIns.isNotEmpty()) appendLine("@OptIn(${optIns.joinToString()})")
        if (jvmExposeBoxed) appendLine("@JvmExposeBoxed")
        if (decl.subclassOptInId != null && (decl.kind == ClassKind.OPEN || decl.kind == ClassKind.ABSTRACT || decl.kind == ClassKind.INTERFACE && !decl.isFunInterface)) {
            appendLine("@SubclassOptInRequired(${classReference(decl.subclassOptInId)}::class)")
        }
        if (decl.consistentCopyVisibility && decl.kind == ClassKind.DATA) appendLine("@ConsistentCopyVisibility")
        // a record has no superclass, and no fields except the ones of its constructor properties
        val record = decl.jvmRecord && decl.kind == ClassKind.DATA && decl.superId == null &&
                decl.members.none { it.isProperty && it.accessor == PropAccessor.FIELD }
        if (record) appendLine("@JvmRecord")
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
                ClassKind.DATA_OBJECT -> "data object "
                ClassKind.ENUM -> "enum class "
                ClassKind.VALUE -> "value class "
                ClassKind.ANNOTATION -> error("printed above")
            }
        )
        // an unnamed companion object is named Companion
        if (!(isCompanion(decl) && decl.unnamedCompanion)) append(decl.name)
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
                val members = listOfNotNull(
                    "override fun toString() = \"b${decl.enumEntryBodyVariant}\"".takeIf { decl.enumEntryBodyVariant != 0 },
                    "override fun ea${decl.id}(): Int = $index".takeIf { decl.enumAbstractMember },
                )
                val body = if (members.isEmpty()) "" else " { ${members.joinToString("; ")} }"
                "$entry$args$body"
            }
            appendLine("    ${entries.joinToString()};")
            if (decl.enumAbstractMember) appendLine("    abstract fun ea${decl.id}(): Int")
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
        for (alias in program.decls.filterIsInstance<TypeAliasDecl>().filter { outerOf(it) == decl }) {
            appendLine(printDecl(alias).trimEnd().prependIndent("    "))
        }
        appendLine("}")
    }

    /** The class [cls] is declared in, `null` for a top-level class or if its outer class does not exist. */
    private fun outerOf(cls: ClassDecl): ClassDecl? = cls.outerId?.let(program::classDecl)

    /** The class [alias] is declared in, `null` for a top-level typealias or if its outer class does not exist or has no body. */
    private fun outerOf(alias: TypeAliasDecl): ClassDecl? = alias.outerId?.let(program::classDecl)?.takeIf { it.kind != ClassKind.ANNOTATION }

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
            root.copy(
                returnType = substitute(root.returnType, INT_TYPE, only = 0),
                params = root.params.map { it.copy(type = substitute(it.type, INT_TYPE, only = 0)) },
            )
        } else {
            root
        }
        val overrideModifiers = when {
            member.overrides != null && member.finalOverride -> "final override "
            member.overrides != null -> "override "
            owner.kind.isInterface -> ""
            member.modality == Modality.OPEN -> "open "
            member.modality == Modality.ABSTRACT -> "abstract "
            else -> ""
        }
        val isAbstract = member.modality == Modality.ABSTRACT && member.overrides == null
        // an inline member has to be final, and an operator or an infix one of their shapes
        val isFinal = member.modality == Modality.FINAL && member.overrides == null && !owner.kind.isInterface
        val isInline = member.inline && isFinal
        val modifiers = overrideModifiers + (if (isInline) "inline " else "") +
                (if (signatureSource.suspend && !signatureSource.isProperty && (!isInline || KnownIssues.inlineOfSeparateCompilation)) "suspend " else "") +
                (if (signatureSource.infix && signatureSource.isBinary) "infix " else "") +
                if (signatureSource.isOperator) "operator " else ""
        // an override has the visibility and the default values of the overridden member
        val jvmStatic = if (member.jvmStatic && owner.kind.isObject && member.overrides == null && member.modality == Modality.FINAL) "@JvmStatic " else ""
        val returnType = signatureSource.returnType.let { if (member.overrides != null && member.covariantReturnType) it.withNullable(false) else it }
        val isProperty = signatureSource.isProperty != (member.overrides != null && member.overridesWithOtherKind)
        // a property of an interface or of a value class has no field, a constant is allowed only in an object
        val accessor = if (owner.kind.isInterface || owner.kind == ClassKind.VALUE || isInline) PropAccessor.GETTER else member.accessor
        val isConst = signatureSource.propKind == PropKind.CONST && owner.kind.isObject && member.overrides == null &&
                member.modality == Modality.FINAL && accessor == PropAccessor.FIELD && isConstType(returnType)
        val isLateinit = signatureSource.propKind == PropKind.LATEINIT && member.overrides == null && !isAbstract &&
                accessor == PropAccessor.FIELD && allowsLateinit(returnType)
        val isVar = signatureSource.propKind == PropKind.VAR || signatureSource.propKind == PropKind.LATEINIT
        // the properties of the companion of an interface may be @JvmField only all together
        val jvmField = member.jvmField && accessor == PropAccessor.FIELD && member.overrides == null && member.modality == Modality.FINAL &&
                !isConst && !isLateinit && !isValueClassType(returnType) && !(isCompanion(owner) && outerOf(owner)?.kind?.isInterface == true)
        // the accessors of a final property only may be renamed
        val accessorJvmNames = isProperty && member.accessorJvmNameVariant != 0 && isFinal && !isConst && !jvmField
        val annotations = jvmStatic + (if (jvmField) "@JvmField " else "") +
                (if (member.transient && isProperty && accessor == PropAccessor.FIELD && !isConst && !isAbstract && !owner.kind.isInterface) "@Transient " else "") +
                (if (member.synchronized && !isProperty && !isAbstract && !owner.kind.isInterface && owner.kind != ClassKind.VALUE &&
                    !signatureSource.suspend) "@Synchronized " else "") +
                if (accessorJvmNames) {
                    "@get:JvmName(\"g${member.id}v${member.accessorJvmNameVariant}\") " +
                            if (isVar && !isLateinit) "@set:JvmName(\"s${member.id}v${member.accessorJvmNameVariant}\") " else ""
                } else ""
        val visibility = annotations + if (member.overrides == null) member.visibility.keyword else ""
        // a private setter is allowed only for a final property
        val setter = if (isProperty && isVar && isFinal && !jvmField) {
            setter(member.visibility, member.setterVisibility, member.setterBodyVariant, hasField = accessor == PropAccessor.FIELD, indent = "        ")
        } else null
        if (isProperty) {
            val keyword = when {
                isConst -> "const val"
                isLateinit -> "lateinit var"
                isVar -> "var"
                else -> "val"
            }
            append("    $visibility$modifiers$keyword ${signatureSource.name}: ${type(returnType)}")
        } else {
            val params = parameters(
                if (isInline) signatureSource.params.map(::inlineFunctionParam) else signatureSource.params,
                printsDefault = { member.overrides == null && memberPrintsDefault(signatureSource, it) },
                inline = isInline,
            )
            val typeParams = if (signatureSource.typeParam) "<M> " else ""
            val receiver = signatureSource.extensionReceiver?.let { "${type(it)}." } ?: ""
            append("    $visibility${modifiers}fun $typeParams$receiver${signatureSource.name}(${params.joinToString()}): ${type(returnType)}")
        }
        if (isAbstract || isProperty && isLateinit) {
            appendLine()
            if (isProperty && isLateinit && member.setterBodyVariant == 0) setter?.let { appendLine(it) }
            return
        }
        if (isProperty && accessor == PropAccessor.FIELD) {
            appendLine(" = ${value(returnType, member.bodyVariant)}")
            setter?.let { appendLine(it) }
        } else if (isProperty) {
            appendLine()
            appendLine("        get() {")
            inlineBody = isInline
            printBody(if (isInline) withoutNestedInlining(member.uses) else member.uses, returnType, member.bodyVariant, indent = "            ")
            inlineBody = false
            appendLine("        }")
            if (isVar) appendLine(setter ?: "        set(value) {}")
        } else {
            appendLine(" {")
            inlineBody = isInline
            printBody(if (isInline) withoutNestedInlining(member.uses) else member.uses, returnType, member.bodyVariant, indent = "        ")
            inlineBody = false
            appendLine("    }")
        }
    }

    /** A constant has a non-nullable primitive or `String` type. */
    private fun memberPrintsDefault(member: MemberFun, param: Param): Boolean =
        param.hasDefault && !param.vararg && param.type !is TypeRef.TypeParamType &&
                (!member.inline || KnownIssues.inlineDefaultValueChange || param.type is TypeRef.BuiltinType)

    /** The code inlined through another inline function or an inline property differs between the separate and the joint compilation. */
    private fun withoutNestedInlining(uses: List<Use>): List<Use> =
        if (KnownIssues.inlineOfSeparateCompilation) uses else uses.filterNot { inlines(it) || isRedundantSafeCast(it) || it is Use.Elvis }

    /** Whether [use] is a safe cast which always succeeds, compiled differently in an inline function from source and from a class file. */
    private fun isRedundantSafeCast(use: Use): Boolean = use is Use.SafeCast && isSubclass(use.valueClassId, use.checkedClassId)

    /** Whether the class [classId] is the class [superId] or inherits from it. */
    private fun isSubclass(classId: Int, superId: Int, depth: Int = 0): Boolean = classId == superId || depth < MAX_HIERARCHY_DEPTH &&
            program.classDecl(classId)?.let { cls -> (listOfNotNull(cls.superId) + cls.interfaceIds).any { isSubclass(it, superId, depth + 1) } } == true

    /** Whether [use] inlines an inline function or an inline property. */
    private fun inlines(use: Use): Boolean = when (use) {
        is Use.CallFun -> (program.decl(use.funId) as? FunDecl)?.inline == true
        is Use.ContractSmartCast -> (program.decl(use.funId) as? FunDecl)?.inline == true
        is Use.ReadProp -> isInlineProperty(use.propId)
        is Use.WhenConst -> isInlineProperty(use.propId)
        is Use.CallMember -> program.member(use.memberId)?.second?.inline == true
        is Use.ImportedMemberCall -> program.member(use.memberId)?.second?.inline == true
        is Use.WhenMember -> program.member(use.memberId)?.second?.inline == true
        is Use.SmartCastMember -> program.member(use.memberId)?.second?.inline == true
        is Use.WriteMember -> program.member(use.memberId)?.second?.inline == true
        is Use.MemberRef -> program.member(use.memberId)?.second?.inline == true
        is Use.FunRef -> (program.decl(use.funId) as? FunDecl)?.inline == true
        is Use.PropRef -> isInlineProperty(use.propId)
        is Use.ChainedCall -> inlines(use.call) || program.member(use.memberId)?.second?.inline == true
        is Use.Elvis -> inlines(use.call)
        else -> false
    }

    /** A constant has a non-nullable primitive or `String` type. */
    private fun isConstType(type: TypeRef): Boolean = type is TypeRef.BuiltinType && !type.nullable && type.builtin in CONST_BUILTINS

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

    private fun parameters(params: List<Param>, printsDefault: (Param) -> Boolean, inline: Boolean = false): List<String> = params.map { param ->
        val vararg = if (printsVararg(param)) "vararg " else ""
        // a nullable or suspend function type parameter of an inline function has to be noinline
        val noinline = if (inline && !param.vararg && functionType(param.type)?.let { it.nullable || it.suspend } == true) "noinline " else ""
        val annotation = param.typeAnnotation?.let { annotationExpression(it) + " " } ?: ""
        "$vararg$noinline${param.name}: $annotation${type(param.type)}" + if (printsDefault(param) && !param.vararg) " = ${value(param.type, param.defaultVariant)}" else ""
    }

    /** Whether [param] is printed as a vararg one: a vararg parameter of a value class type is prohibited. */
    private fun printsVararg(param: Param): Boolean = param.vararg && !isValueClassType(param.type)

    /**
     * A parameter of an inline function: a function type parameter is an inline lambda, which cannot be nullable or suspend without
     * `noinline`, generated only with [KnownIssues.inlineOfSeparateCompilation].
     */
    private fun inlineFunctionParam(param: Param): Param {
        // a vararg parameter is an array, not an inline lambda
        val type = param.type.takeIf { !param.vararg }?.let(::functionType) ?: return param
        return if (KnownIssues.inlineOfSeparateCompilation) param else param.copy(type = type.copy(nullable = false, suspend = false))
    }

    /** The function type which [type] is, possibly through typealiases. */
    private fun functionType(type: TypeRef, depth: Int = 0): TypeRef.FunctionType? = when {
        type is TypeRef.FunctionType -> type
        type is TypeRef.AliasType && depth < MAX_ALIAS_DEPTH -> (program.decl(type.aliasId) as? TypeAliasDecl)?.let { alias ->
            val target = if (alias.generic) substitute(alias.target, type.typeArg ?: INT_TYPE) else alias.target
            functionType(target, depth + 1)?.let { if (type.nullable) it.copy(nullable = true) else it }
        }
        else -> null
    }

    /**
     * The arguments of a call passing the [params] without a printed default value, by the names in [argNames] if [named],
     * or if a parameter with a default value or a vararg one precedes them. A text-stable call passes its own [stable] arguments instead.
     */
    private fun arguments(
        params: List<Param>,
        printsDefault: (Param) -> Boolean,
        argNames: List<String>,
        named: Boolean,
        typeArg: TypeRef? = null,
        stable: List<Param>? = null,
    ): Pair<List<String>, Boolean> {
        val passed = stable?.withIndex()?.toList() ?: params.withIndex().filterNot { printsDefault(it.value) }
        val positional = !named && (stable != null || passed.withIndex().all { [index, param] -> param.index == index }) &&
                passed.dropLast(1).none { it.value.vararg }
        val args = passed.map { [index, param] ->
            val type = substitute(param.type, typeArg)
            val value = if (printsVararg(param)) "*${arrayLiteral(type, primitive = param.type !is TypeRef.TypeParamType)}" else value(type, 0)
            if (positional) value else "${if (stable != null) param.name else argNames.getOrNull(index) ?: param.name} = $value"
        }
        return args to positional
    }

    /**
     * Substitutes the type parameters with [typeArg] in [type], only the one with the index [only] if it is not `null`:
     * the calls pass the same type argument for all the type parameters.
     */
    private fun substitute(type: TypeRef, typeArg: TypeRef?, only: Int? = null): TypeRef = if (typeArg == null) type else when (type) {
        is TypeRef.TypeParamType -> when {
            only != null && type.index != only -> type
            type.definitelyNonNull -> typeArg.withNullable(false)
            else -> typeArg.withNullable(type.nullable || typeArg.nullable)
        }
        is TypeRef.ListType -> type.copy(element = substitute(type.element, typeArg, only))
        is TypeRef.ClassType -> type.copy(typeArg = type.typeArg?.let { substitute(it, typeArg, only) })
        is TypeRef.AliasType -> type.copy(typeArg = type.typeArg?.let { substitute(it, typeArg, only) })
        is TypeRef.FunctionType -> type.copy(
            params = type.params.map { substitute(it, typeArg, only) },
            returnType = substitute(type.returnType, typeArg, only),
            receiver = type.receiver?.let { substitute(it, typeArg, only) },
        )
        is TypeRef.ArrayType -> type.copy(element = substitute(type.element, typeArg, only))
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
        result: String? = null,
    ) {
        // a lambda passed to an inline function may return from a block body, not from an expression body
        val outerReturnValue = currentReturnValue
        currentReturnValue = if (implicitResult) null else result ?: value(returnType, variant)
        for (use in uses.filterNot(::reproducesExcludedIssue)) {
            appendLine("${indent}println(${useExpression(use)})")
        }
        currentReturnValue = outerReturnValue
        for (statement in extraStatements) {
            appendLine("$indent$statement")
        }
        // a lambda as the last expression would be a trailing lambda of the previous statement
        val printedResult = (result ?: value(returnType, variant)).let { if (implicitResult && it.startsWith("{")) "($it)" else it }
        appendLine("$indent${if (implicitResult) "" else "return "}$printedResult")
    }

    /** The value the function whose body is being printed may return, `null` outside of a block body. */
    private var currentReturnValue: String? = null

    /** Whether [use] reproduces one of the [KnownIssues] which are excluded. */
    private fun reproducesExcludedIssue(use: Use): Boolean = when (use) {
        is Use.SamConversion -> !KnownIssues.funInterfaceChange
        is Use.WhenSealed -> !KnownIssues.indirectSealedSubclass && hasSealedSubclass(use.sealedId)
        is Use.Elvis -> !KnownIssues.inlineOfSeparateCompilation && inlines(use.call)
        is Use.WriteMember -> !KnownIssues.jvmFieldOfObject && isJvmFieldOfObject(use.classId, use.memberId)
        is Use.MemberRef -> !KnownIssues.jvmFieldOfObject && isJvmFieldOfObject(use.classId, use.memberId) ||
                !KnownIssues.constPropertyReference && program.member(use.memberId)?.second?.propKind == PropKind.CONST
        is Use.FunRef -> !KnownIssues.multifileReference && program.fileOf(use.funId)?.multifile == true
        is Use.PropRef -> !KnownIssues.multifileReference && program.fileOf(use.propId)?.multifile == true
        is Use.ImportedEnumEntry -> !KnownIssues.importOfPrivateEnumEntryClass &&
                program.classDecl(use.enumId)?.let { it.enumEntryBodyVariant != 0 && isPrivateOrNestedInPrivate(it) } == true
        else -> false
    }

    private fun isPrivateOrNestedInPrivate(cls: ClassDecl, depth: Int = 0): Boolean = depth < MAX_NESTING &&
            (cls.visibility == Visibility.PRIVATE || cls.outerId?.let(program::classDecl)?.let { isPrivateOrNestedInPrivate(it, depth + 1) } == true)

    /** Whether a subclass of the class [classId], possibly an indirect one, is sealed. */
    private fun hasSealedSubclass(classId: Int, depth: Int = 0): Boolean = depth < MAX_HIERARCHY_DEPTH &&
            program.directSubclasses(classId).any { it.kind.isSealed || hasSealedSubclass(it.id, depth + 1) }

    private fun isJvmFieldOfObject(classId: Int, memberId: Int): Boolean =
        program.classDecl(classId)?.kind?.isObject == true && program.member(memberId)?.second?.jvmField == true

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
        is Use.CallMember -> callMember(use)
        is Use.WhenSealed -> {
            // a generic class is checked with a star projection, as its type argument is erased
            val branches = use.branchClassIds.map { "is ${typeReference(it, star = true)} -> \"${it}\"" } + listOfNotNull("else -> \"e\"".takeIf { use.withElse })
            "when (val v: ${typeReference(use.sealedId, star = true)} = ${instance(use.scrutineeClassId)}) { ${branches.joinToString("; ")} }"
        }
        is Use.WhenEnum -> {
            val enumName = classReference(use.enumId)
            val branches = use.entries.map { "$enumName.$it -> \"$it\"" } + listOfNotNull("else -> \"e\"".takeIf { use.withElse })
            "when ($enumName.${if (use.viaValues) "values()" else "entries"}.first()) { ${branches.joinToString("; ")} }"
        }
        is Use.FunRef -> {
            val decl = program.decl(use.funId) as? FunDecl
            val reference = decl?.let { (it.receiverClassId?.let(::typeReference) ?: "") + "::" + reference(it) } ?: "::${unresolved(use.funId)}"
            // a reference to an extension of an object is bound to the object, so it has no receiver
            val boundReceiver = (use.paramTypes.firstOrNull() as? TypeRef.ClassType)?.takeIf { decl?.receiverClassId != null }
                ?.let { program.classDecl(it.classId)?.kind?.isObject } == true
            val paramTypes = if (boundReceiver) use.paramTypes.drop(1) else use.paramTypes
            "run { val r: ${type(TypeRef.FunctionType(paramTypes, use.returnType))} = $reference; r }"
        }
        is Use.PropRef -> {
            val prop = program.decl(use.propId)
            "run { val r: kotlin.reflect.KProperty0<${type(use.type)}> = ::${prop?.let(::reference) ?: unresolved(use.propId)}; r }"
        }
        is Use.MemberRef -> {
            val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
            val owner = typeReference(use.classId)
            val reference = "$owner::${member?.name ?: unresolved(use.memberId)}"
            // a reference to a member of an object is bound to the object, so it has no receiver
            val bound = program.classDecl(use.classId)?.kind?.isObject == true
            val receiver = if (bound) emptyList() else listOf(owner)
            when {
                use.isProperty && bound -> "run { val r: kotlin.reflect.KProperty0<${type(use.returnType)}> = $reference; r }"
                use.isProperty -> "run { val r: kotlin.reflect.KProperty1<$owner, ${type(use.returnType)}> = $reference; r }"
                else -> "run { val r: (${(receiver + use.paramTypes.map { type(it) }).joinToString()}) -> ${type(use.returnType)} = $reference; r }"
            }
        }
        is Use.ClassRef -> "${classReference(use.classId)}::class.simpleName"
        is Use.IsCheck -> "run { val x: ${typeReference(use.valueClassId)} = ${instance(use.valueClassId)}; x is ${typeReference(use.checkedClassId, star = true)} }"
        is Use.SafeCast -> "(${instance(use.valueClassId)} as? ${typeReference(use.checkedClassId, star = true)})"
        is Use.ChainedCall -> {
            val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
            val access = when {
                member == null -> "${unresolved(use.memberId)}()"
                member.isProperty -> member.name
                else -> "${member.name}(${arguments(member.params, { memberPrintsDefault(member, it) }, emptyList(), named = true, INT_TYPE).first.joinToString()})"
            }
            "(${useExpression(use.call)})${if (use.safe) "?." else "."}$access"
        }
        is Use.Elvis -> "(${useExpression(use.call)} ?: TODO())"
        is Use.WriteProp -> {
            val prop = program.decl(use.propId) as? PropDecl
            if (prop == null) unresolved(use.propId) else "run { ${propertyRead(prop)} = ${value(prop.type, 0)}; 0 }"
        }
        is Use.WriteMember -> {
            val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
            if (member == null) unresolved(use.memberId)
            else "run { ${instance(use.classId)}.${member.name} = ${value(substitute(member.returnType, INT_TYPE), 0)}; 0 }"
        }
        is Use.ImportedMemberCall -> {
            val obj = program.classDecl(use.objectId)
            val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
            obj?.let { imports += "${qualifiedName(it)}.${use.memberName}" }
            when {
                obj == null || member == null -> "${unresolved(use.memberId)}()"
                member.isProperty -> use.memberName
                else -> "${use.memberName}(${arguments(member.params, { memberPrintsDefault(member, it) }, emptyList(), named = true, INT_TYPE).first.joinToString()})"
            }
        }
        is Use.ImportedEnumEntry -> {
            program.classDecl(use.enumId)?.let { imports += "${qualifiedName(it)}.${use.entry}" }
            use.entry
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
                        decl.params.drop(1).filterNot(decl::printsDefault).map {
                            "${it.name} = ${if (printsVararg(it)) "*${arrayLiteral(it.type, primitive = it.type !is TypeRef.TypeParamType)}" else value(it.type, 0)}"
                        } +
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
        val [args, positional] = arguments(decl.params, decl::printsDefault, use.argNames, use.named, use.typeArg, use.args)
        val receiver = decl.receiverClassId?.let { "${instance(it)}." } ?: ""
        val plainName = reference(decl)
        // the calls pass the same type argument for both type parameters
        val name = if (decl.typeParam == null) plainName else "$plainName<${type(use.typeArg)}${if (decl.typeParam2) ", ${type(use.typeArg)}" else ""}>"
        // an inlined lambda cannot be serializable, and a serializable lambda in an inline function is a class copied to its call sites,
        // see KnownIssues.inlineFunOwnedClass
        val serializable = if (use.serializableLambda && !decl.inline && (!inlineBody || KnownIssues.inlineFunOwnedClass)) "@JvmSerializableLambda " else ""
        val lambda = decl.lambda?.let {
            when (use.lambdaForm) {
                LambdaForm.VALUE -> null
                // the label of a lambda is the name of the called function
                LambdaForm.LABELED_RETURN -> "{ return@$plainName ${use.lambdaVariant} }"
                // the call may resolve to a more specific overload, which is not inline
                LambdaForm.NON_LOCAL_RETURN -> currentReturnValue
                    ?.takeIf { decl.inline && program.decls.none { other -> other is FunDecl && other.name == decl.name && !other.inline } }
                    ?.let { "{ if (false) return $it; ${use.lambdaVariant} }" }
            } ?: "{ ${use.lambdaVariant} }"
        }?.let { serializable + it }
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
            decl.isOperator && (decl.operatorKind == OperatorKind.GET_VALUE || decl.operatorKind == OperatorKind.PROVIDE_DELEGATE) ->
                "$operand.${reference(decl)}(null, String::length)"
            decl.isOperator && decl.operatorKind == OperatorKind.SET_VALUE -> "$operand.${reference(decl)}(null, String::length, ${value(decl.returnType, 0)})"
            decl.isOperator && use.form == CallForm.OPERATOR && (decl.typeParam == null || binary) -> when (decl.operatorKind) {
                OperatorKind.PLUS -> "($operand + $binaryArg)"
                OperatorKind.GET -> "$operand[$binaryArg]"
                OperatorKind.COMPARE_TO -> "($operand < $binaryArg)"
                OperatorKind.INVOKE -> "$operand$regularArgs"
                OperatorKind.COMPONENT1 -> "run { val [c] = $operand; c }"
                OperatorKind.GET_VALUE, OperatorKind.SET_VALUE, OperatorKind.PROVIDE_DELEGATE -> error("handled above")
            }
            else -> "$receiver$name$regularArgs"
        }
    }

    /** A call of a member, in the infix or the operator form of the call if the member has the modifier. */
    private fun callMember(use: Use.CallMember): String {
        val member = program.member(use.memberId)?.second?.let { it.overrides?.let(::rootOverridden) ?: it }
        val instance = instance(use.classId)
        // an implicit call may resolve to another function with the same name
        fun inScope(call: String) = if (use.implicit) "with($instance) { $call }" else "$instance.$call"
        if (member == null) return inScope("${unresolved(use.memberId)}()")
        if (member.isProperty) return inScope(member.name)
        // the calls pass `Int` for the type parameters of the class and of the member
        val args = arguments(member.params, { memberPrintsDefault(member, it) }, use.argNames, use.named, INT_TYPE, use.args).first.joinToString()
        val typeArgs = if (member.typeParam) "<Int>" else ""
        val binaryArg by lazy { value(substitute(member.params.single().type, INT_TYPE), 0) }
        return when {
            // an infix call cannot pass the type argument
            member.infix && member.isBinary && !member.typeParam && use.form == CallForm.INFIX -> "($instance ${member.name} $binaryArg)"
            member.isOperator && use.form == CallForm.OPERATOR -> when (member.operatorKind) {
                OperatorKind.PLUS -> "($instance + $binaryArg)"
                OperatorKind.GET -> "$instance[$binaryArg]"
                OperatorKind.COMPARE_TO -> "($instance < $binaryArg)"
                OperatorKind.INVOKE -> "$instance($args)"
                OperatorKind.COMPONENT1 -> "run { val [c] = $instance; c }"
                OperatorKind.GET_VALUE, OperatorKind.SET_VALUE, OperatorKind.PROVIDE_DELEGATE -> "$instance.${member.name}($args)"
            }
            // a member extension is called in the scope of an instance of its class
            member.extensionReceiver != null -> "with($instance) { ${value(member.extensionReceiver, 0)}.${member.name}$typeArgs($args) }"
            else -> inScope("${member.name}$typeArgs($args)")
        }
    }

    /** The qualified name of the class [cls], through its outer classes. */
    private fun qualifiedName(cls: ClassDecl): String {
        val outer = outerOf(cls) ?: return "${program.fileOf(cls.id)?.pkg}.${cls.name}"
        return "${qualifiedName(outer)}.${if (isCompanion(cls) && cls.unnamedCompanion) "Companion" else cls.name}"
    }

    /** The getValue or provideDelegate operator a delegated property delegates to, `null` if it is not an operator any more. */
    private fun delegateOf(prop: PropDecl): FunDecl? = (prop.delegateFunId?.let(program::decl) as? FunDecl)
        ?.takeIf { it.isOperator && (it.operatorKind == OperatorKind.GET_VALUE || it.operatorKind == OperatorKind.PROVIDE_DELEGATE) }

    /** The setValue operator with the receiver of the [delegateOf] operator of [prop]. */
    private fun setterDelegateOf(prop: PropDecl): FunDecl? {
        val receiver = delegateOf(prop)?.receiverClassId ?: return null
        return program.decls.filterIsInstance<FunDecl>()
            .firstOrNull { it.isOperator && it.operatorKind == OperatorKind.SET_VALUE && it.receiverClassId == receiver }
    }

    private fun type(type: TypeRef): String {
        val base = when (type) {
            is TypeRef.BuiltinType -> type.builtin.kotlinName
            is TypeRef.ClassType -> typeReference(type.classId, type.typeArg, type.star, type.projection)
            is TypeRef.AliasType -> {
                val alias = program.decl(type.aliasId) as? TypeAliasDecl
                when {
                    alias == null -> program.decl(type.aliasId)?.let(::reference) ?: unresolved(type.aliasId)
                    alias.generic -> "${reference(alias)}<${type(type.typeArg ?: INT_TYPE)}>"
                    else -> reference(alias)
                }
            }
            // a type parameter with a non-null bound is not made definitely non-null
            is TypeRef.TypeParamType -> TYPE_PARAM_NAMES[type.index] + if (type.definitelyNonNull && type.index !in nonNullTypeParams) " & Any" else ""
            is TypeRef.ListType -> "List<${if (type.wildcard) "@JvmWildcard " else ""}${type(type.element)}>"
            is TypeRef.FunctionType -> {
                "${if (type.suspend) "suspend " else ""}${type.receiver?.let(::receiverPrefix) ?: ""}(${type.params.joinToString { type(it) }}) -> ${type(type.returnType)}"
            }
            is TypeRef.ArrayType -> "Array<${type(type.element)}>"
        }
        return when {
            // a definitely non-null type cannot be nullable
            type is TypeRef.TypeParamType && type.definitelyNonNull && type.index !in nonNullTypeParams -> base
            type is TypeRef.FunctionType && type.nullable -> "($base)?"
            type.nullable -> "$base?"
            else -> base
        }
    }

    /** The indices of the type parameters in scope whose bounds are not nullable. */
    private var nonNullTypeParams: Set<Int> = emptySet()

    /** The receiver of a function type followed by the dot: a function type as a receiver is parenthesized, a nullable one already is. */
    private fun receiverPrefix(receiver: TypeRef): String =
        type(receiver).let { if (receiver is TypeRef.FunctionType && !receiver.nullable) "($it)." else "$it." }

    /**
     * A reference to the class [classId] as a type, with the type argument of a generic class: [typeArg], `Int` by default,
     * projected with [projection], or a star.
     */
    private fun typeReference(classId: Int, typeArg: TypeRef? = null, star: Boolean = false, projection: Variance? = null): String {
        val reference = classReference(classId)
        val cls = program.classDecl(classId)
        if (cls == null || !cls.isGeneric) return reference
        return "$reference<${if (star) "*" else (projection?.keyword ?: "") + type(typeArgument(cls, typeArg))}>"
    }

    /**
     * The type argument of the generic [cls], `Int` by default: the type argument of a class with the `Any` bound cannot be nullable,
     * and a typealias may expand to a nullable type.
     */
    private fun typeArgument(cls: ClassDecl, typeArg: TypeRef?): TypeRef {
        val argument = typeArg ?: INT_TYPE
        return when {
            !cls.typeParamNonNull -> argument
            argument is TypeRef.AliasType -> INT_TYPE
            else -> argument.withNullable(false)
        }
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
            // the parameters are typed, so that the lambda needs no expected type, and an anonymous function has the type with the receiver,
            // parenthesized not to end with the next statement; a suspend one cannot be anonymous, its lambda needs the expected type
            is TypeRef.FunctionType -> if (type.receiver != null && !type.suspend) {
                val params = type.params.withIndex().joinToString { [index, param] -> "p$index: ${type(param)}" }
                "(fun ${receiverPrefix(type.receiver)}($params): ${type(type.returnType)} = ${value(type.returnType, variant, depth + 1)})"
            } else {
                val params = type.params.joinToString { "_: ${type(it)}" }
                "{ ${if (params.isEmpty()) "" else "$params -> "}${value(type.returnType, variant, depth + 1)} }"
            }
            is TypeRef.ArrayType -> "arrayOf<${type(type.element)}>(${value(type.element, variant, depth + 1)})"
        }
    }

    /**
     * The array passed with a spread to a vararg parameter of [elementType], a [primitive] one for a parameter declared with a primitive type,
     * possibly through a typealias, and not with a type parameter.
     */
    private fun arrayLiteral(elementType: TypeRef, primitive: Boolean): String {
        val element = value(elementType, 0)
        return when ((expanded(elementType) as? TypeRef.BuiltinType)?.takeIf { primitive && !it.nullable }?.builtin) {
            Builtin.INT -> "intArrayOf($element)"
            Builtin.LONG -> "longArrayOf($element)"
            Builtin.BOOLEAN -> "booleanArrayOf($element)"
            else -> "arrayOf<${type(elementType)}>($element)"
        }
    }

    /** [type] with its typealiases expanded, nullable if a typealias is. */
    private fun expanded(type: TypeRef, depth: Int = 0): TypeRef = when {
        type is TypeRef.AliasType && depth < MAX_ALIAS_DEPTH -> (program.decl(type.aliasId) as? TypeAliasDecl)?.let { alias ->
            val target = expanded(if (alias.generic) substitute(alias.target, type.typeArg ?: INT_TYPE) else alias.target, depth + 1)
            if (type.nullable) target.withNullable(true) else target
        } ?: type
        else -> type
    }

    /** An instance of the class [classId], with the type argument [typeArg] of a generic class, `Int` by default. */
    private fun instance(classId: Int, typeArg: TypeRef? = null): String {
        val cls = program.classDecl(classId) ?: return unresolved(classId)
        // a constructor parameter may have the type of its class
        if (instanceDepth > MAX_INSTANCE_DEPTH) return "TODO()"
        instanceDepth++
        try {
            val arg = typeArgument(cls, typeArg)
            val typeArgs = if (cls.isGeneric) "<${type(arg)}>" else ""
            if (isInner(cls)) return "${instance(outerOf(cls)!!.id)}.${cls.name}$typeArgs(${constructorArguments(cls, arg)})"
            val name = reference(cls) + typeArgs
            return when (cls.kind) {
                ClassKind.OBJECT, ClassKind.DATA_OBJECT -> reference(cls)
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
        "${annotationExpression(use)}\n"
    }

    /** An annotation, with the arguments of the parameters the annotation class had when the use was created. */
    private fun annotationExpression(use: AnnotationUse): String {
        val args = buildList {
            add("\"a${use.argVariant}\"")
            use.classArgId?.let { add("k = ${classReference(it)}::class") }
            use.enumArg?.let { [enumId, entry] -> add("e = ${classReference(enumId)}.$entry") }
            use.arrayArg?.let { add("a = [$it]") }
        }
        return "@${use.useSiteTarget?.let { "${it.keyword}:" } ?: ""}${classReference(use.annotationId)}(${args.joinToString()})"
    }

    /** The enum class of the enum parameter of an annotation class, `null` if it has none or the enum has no entries. */
    private fun annotationEnumOf(cls: ClassDecl): ClassDecl? =
        cls.annotationEnumId?.takeIf { AnnotationParam.ENUM in cls.annotationParams }?.let(program::classDecl)
            ?.takeIf { it.kind == ClassKind.ENUM && it.enumEntries.isNotEmpty() }

    private fun classReference(classId: Int): String = program.decl(classId)?.let(::reference) ?: unresolved(classId)

    /**
     * Returns the name of the top-level [decl] and imports it if it is declared in another package, explicitly, with an alias, or with a star.
     * A nested class is referenced through its outer class.
     */
    private fun reference(decl: Decl): String {
        if (decl is ClassDecl) outerOf(decl)?.let { return "${reference(it)}.${if (isCompanion(decl) && decl.unnamedCompanion) "Companion" else decl.name}" }
        if (decl is TypeAliasDecl) outerOf(decl)?.let { return "${reference(it)}.${decl.name}" }
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
        private const val MAX_HIERARCHY_DEPTH = 16
        private const val MAX_NESTING = 16
        private const val MAX_INSTANCE_DEPTH = 4
        private val INT_TYPE: TypeRef = TypeRef.BuiltinType(Builtin.INT)
        private val UNIT_TYPE: TypeRef = TypeRef.BuiltinType(Builtin.UNIT)
        private const val LAMBDA_PARAM = "block"
        private const val ALIAS_SUFFIX = "Alias"

        /** The names of the type parameters by [TypeRef.TypeParamType.index]. */
        private val TYPE_PARAM_NAMES = listOf("T", "U", "M")

        private val CONST_BUILTINS = setOf(Builtin.INT, Builtin.LONG, Builtin.STRING, Builtin.BOOLEAN)

        /** The kinds of classes which may be inner. */
        private val INNER_KINDS = setOf(ClassKind.FINAL, ClassKind.OPEN, ClassKind.ABSTRACT)

        /** The visibilities from the least to the most restrictive. */
        private val RESTRICTIVENESS = listOf(Visibility.PUBLIC, Visibility.INTERNAL, Visibility.PROTECTED, Visibility.PRIVATE)

        fun print(program: Program): Map<String, String> {
            val printer = ProgramPrinter(program)
            return program.files.associate { it.name to printer.printFile(it) }
        }
    }
}
