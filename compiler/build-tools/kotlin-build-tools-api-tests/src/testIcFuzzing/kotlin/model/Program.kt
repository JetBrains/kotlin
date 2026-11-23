/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.model

/**
 * A model of a single-module Kotlin program that the fuzzer generates and edits.
 *
 * Declarations reference each other by [Decl.id] / [MemberFun.id], never by name, so that a rename or a move is a change of a single
 * declaration and every reference is printed consistently by [ProgramPrinter]. A reference to an id that no longer exists is printed
 * as an unresolved name, which makes the program fail to compile in the same way for the incremental and for the clean build.
 */
data class Program(val files: List<SourceFile>, val nextId: Int) {
    val decls: List<Decl> get() = files.flatMap { it.decls }

    val classes: List<ClassDecl> get() = decls.filterIsInstance<ClassDecl>()

    /** The uses of all the functions and members. */
    fun allUses(): List<Use> = decls.filterIsInstance<FunDecl>().flatMap { it.uses } + classes.flatMap { cls -> cls.members.flatMap { it.uses } }

    fun decl(id: Int): Decl? = files.firstNotNullOfOrNull { file -> file.decls.firstOrNull { it.id == id } }

    fun classDecl(id: Int): ClassDecl? = decl(id) as? ClassDecl

    fun fileOf(id: Int): SourceFile? = files.firstOrNull { file -> file.decls.any { it.id == id } }

    fun member(id: Int): Pair<ClassDecl, MemberFun>? =
        classes.firstNotNullOfOrNull { cls -> cls.members.firstOrNull { it.id == id }?.let { cls to it } }

    fun file(name: String): SourceFile? = files.firstOrNull { it.name == name }

    /** The classes whose supertype is [classId]. */
    fun directSubclasses(classId: Int): List<ClassDecl> = classes.filter { it.superId == classId || classId in it.interfaceIds }

    /** [classId] followed by its superclasses, nearest first. Stops on a missing or a cyclic supertype. */
    fun superChain(classId: Int): List<ClassDecl> {
        val chain = mutableListOf<ClassDecl>()
        var current = classDecl(classId)
        while (current != null && current !in chain) {
            chain += current
            current = current.superId?.let(::classDecl)
        }
        return chain
    }

    /** [classId] followed by all its supertypes, the superclasses and the interfaces, nearest first. Skips the missing ones. */
    fun supertypes(classId: Int): List<ClassDecl> {
        val result = linkedSetOf<ClassDecl>()
        val queue = ArrayDeque(listOfNotNull(classDecl(classId)))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!result.add(current)) continue
            queue += listOfNotNull(current.superId).plus(current.interfaceIds).mapNotNull(::classDecl)
        }
        return result.toList()
    }

    fun replaceDecl(id: Int, transform: (Decl) -> Decl): Program =
        copy(files = files.map { file -> file.copy(decls = file.decls.map { if (it.id == id) transform(it) else it }) })

    fun replaceFile(name: String, transform: (SourceFile) -> SourceFile): Program =
        copy(files = files.map { if (it.name == name) transform(it) else it })

    fun withNextId(): Pair<Program, Int> = copy(nextId = nextId + 1) to nextId

    companion object {
        val EMPTY = Program(emptyList(), nextId = 1)
    }
}

/**
 * @property starImports whether the declarations of the other packages are imported with star imports instead of explicit ones
 * @property aliasedIds the declarations of the other packages imported with an alias
 * @property blankLines the number of additional blank lines before a declaration, by its id
 * @property jvmNameVariant if not 0, the file facade is named `Facade<variant>` with `@file:JvmName`
 * @property multifile whether the facade named with `@file:JvmName` is a `@JvmMultifileClass`
 */
data class SourceFile(
    val name: String,
    val pkg: String,
    val decls: List<Decl>,
    val commentVariant: Int = 0,
    val starImports: Boolean = false,
    val aliasedIds: Set<Int> = emptySet(),
    val blankLines: Map<Int, Int> = emptyMap(),
    val jvmNameVariant: Int = 0,
    val multifile: Boolean = false,
)

enum class Builtin(val kotlinName: String) {
    INT("Int"), LONG("Long"), STRING("String"), BOOLEAN("Boolean"), ANY("Any"),

    /** The rare types, generated only by [Edits] on purpose. */
    UNIT("Unit"), NOTHING("Nothing"), INT_ARRAY("IntArray");

    fun literal(variant: Int): String = when (this) {
        INT, ANY -> "$variant"
        LONG -> "${variant}L"
        STRING -> "\"s$variant\""
        BOOLEAN -> if (variant % 2 == 0) "false" else "true"
        UNIT -> "Unit"
        NOTHING -> "TODO()"
        INT_ARRAY -> "intArrayOf($variant)"
    }
}

sealed interface TypeRef {
    val nullable: Boolean

    fun withNullable(nullable: Boolean): TypeRef = when (this) {
        is BuiltinType -> copy(nullable = nullable)
        is ClassType -> copy(nullable = nullable)
        is AliasType -> copy(nullable = nullable)
        is TypeParamType -> copy(nullable = nullable)
        is ListType -> copy(nullable = nullable)
        is FunctionType -> copy(nullable = nullable)
        is ArrayType -> copy(nullable = nullable)
    }

    data class BuiltinType(val builtin: Builtin, override val nullable: Boolean = false) : TypeRef

    /**
     * A class type. The type argument of a generic class is [typeArg], `Int` if it is `null`, or a star projection if [star];
     * a non-generic class ignores it. The type argument is projected with [projection] at the use site.
     */
    data class ClassType(
        val classId: Int,
        override val nullable: Boolean = false,
        val typeArg: TypeRef? = null,
        val star: Boolean = false,
        val projection: Variance? = null,
    ) : TypeRef

    /** A typealias type, with the type argument [typeArg] of a generic typealias, `Int` if it is `null`. */
    data class AliasType(val aliasId: Int, override val nullable: Boolean = false, val typeArg: TypeRef? = null) : TypeRef

    /**
     * A type parameter, substituted with the type argument at the use sites: `T` of the function, of the class or of the typealias
     * the type is used in for the [index] 0, `U` of a function for 1, `M` of a member function for 2.
     * It is the definitely non-null `T & Any` if [definitelyNonNull].
     */
    data class TypeParamType(override val nullable: Boolean = false, val index: Int = 0, val definitelyNonNull: Boolean = false) : TypeRef

    /** A list type, whose element is a `@JvmWildcard` type argument if [wildcard]. */
    data class ListType(val element: TypeRef, override val nullable: Boolean = false, val wildcard: Boolean = false) : TypeRef

    /**
     * A function type with the [receiver] if it is not `null`. Its values are lambdas with explicitly typed parameters,
     * or anonymous functions for a function type with a receiver, which need no expected type.
     */
    data class FunctionType(
        val params: List<TypeRef>,
        val returnType: TypeRef,
        val suspend: Boolean = false,
        override val nullable: Boolean = false,
        val receiver: TypeRef? = null,
    ) : TypeRef

    data class ArrayType(val element: TypeRef, override val nullable: Boolean = false) : TypeRef
}

enum class Visibility(val keyword: String) {
    PUBLIC(""), INTERNAL("internal "), PRIVATE("private "), PROTECTED("protected ");

    companion object {
        /** The visibilities of the top-level declarations. */
        val TOP_LEVEL: List<Visibility> = listOf(PUBLIC, INTERNAL, PRIVATE)
    }
}

/**
 * @property vararg whether the parameter is a `vararg` one, which has no default value; its argument is passed with a spread
 * @property typeAnnotation the annotation of the type of the parameter, with an annotation class targeting types
 */
data class Param(
    val name: String,
    val type: TypeRef,
    val hasDefault: Boolean,
    val defaultVariant: Int = 0,
    val vararg: Boolean = false,
    val typeAnnotation: AnnotationUse? = null,
)

/** A parameter of a primary constructor, which declares a property when [isProperty]. */
data class CtorParam(val name: String, val type: TypeRef, val hasDefault: Boolean, val isProperty: Boolean, val defaultVariant: Int = 0) {
    fun asParam() = Param(name, type, hasDefault, defaultVariant)
}

/**
 * An annotation of a declaration with the annotation class [annotationId], passing [argVariant] as its `String` argument,
 * and the arguments of the other parameters it had when the use was created: the class [classArgId], the enum entry [enumArg]
 * and the array [arrayArg]. A property may be annotated at the [useSiteTarget].
 */
data class AnnotationUse(
    val annotationId: Int,
    val argVariant: Int,
    val classArgId: Int? = null,
    val enumArg: Pair<Int, String>? = null,
    val arrayArg: Int? = null,
    val useSiteTarget: UseSiteTarget? = null,
)

enum class UseSiteTarget(val keyword: String) { FIELD("field"), GET("get") }

/** The parameters an annotation class may have in addition to `v: String`. */
enum class AnnotationParam { KCLASS, ENUM, ARRAY }

/** The form of the lambda argument of a call: returning a value, returning it with a label, or with a never taken non-local return. */
enum class LambdaForm { VALUE, LABELED_RETURN, NON_LOCAL_RETURN }

/** A function of the standard library a top-level function of a package shadows. */
enum class StdlibShadow { PRINTLN, LIST_OF }

/** An expression printed as `println(...)` inside a function body. */
sealed interface Use {
    /**
     * A call of the function [funId]. The arguments are passed by the names the parameters had when the use was created, [argNames],
     * so that a call stops compiling when a parameter is renamed. A [named] call passes all its arguments by name if possible.
     * The lambda argument returns [lambdaVariant] in the [lambdaForm], and is a `@JvmSerializableLambda` if [serializableLambda]
     * and it is not inlined.
     *
     * A text-stable call has the [args] the function had when the use was created: its source does not change with the function,
     * so a later edit of the parameters keeps it as it was, and it may stop compiling. Otherwise it passes the current parameters.
     */
    data class CallFun(
        val funId: Int,
        val argNames: List<String> = emptyList(),
        val named: Boolean = true,
        val lambdaVariant: Int = 0,
        val form: CallForm = CallForm.REGULAR,
        val typeArg: TypeRef = TypeRef.BuiltinType(Builtin.INT),
        val scopeClassId: Int? = null,
        val args: List<Param>? = null,
        val lambdaForm: LambdaForm = LambdaForm.VALUE,
        val serializableLambda: Boolean = false,
    ) : Use
    data class ReadProp(val propId: Int) : Use
    /** A creation of an instance of [classId], through the secondary constructor if [viaSecondary] and the class has one. */
    data class Construct(val classId: Int, val viaSecondary: Boolean = false) : Use

    /**
     * A call of the member [memberId] (possibly inherited) on an instance of [classId], the arguments are passed as in [CallFun].
     * An [implicit] call is made in a `with` block on the instance, so a function with the same name may win the resolution.
     * A call of an operator or of an infix member may be made in its [form].
     */
    data class CallMember(
        val classId: Int,
        val memberId: Int,
        val argNames: List<String> = emptyList(),
        val named: Boolean = true,
        val implicit: Boolean = false,
        val args: List<Param>? = null,
        val form: CallForm = CallForm.REGULAR,
    ) : Use

    /** An exhaustive `when` over the sealed class [sealedId], branches are fixed when the use is created, with an `else` branch if [withElse]. */
    data class WhenSealed(val sealedId: Int, val scrutineeClassId: Int, val branchClassIds: List<Int>, val withElse: Boolean = false) : Use

    /** An exhaustive `when` over the enum class [enumId], branches are fixed when the use is created. The scrutinee is read [viaValues]. */
    data class WhenEnum(val enumId: Int, val entries: List<String>, val viaValues: Boolean = false, val withElse: Boolean = false) : Use

    /** A `when` with a branch condition reading the property [propId], which is inlined by the compiler when it is a constant. */
    data class WhenConst(val propId: Int) : Use

    /** A SAM conversion of a lambda to the interface [interfaceId], which compiles only if it is a `fun interface`. */
    data class SamConversion(val interfaceId: Int) : Use

    /**
     * A call of the function [funId] with `null` as its first argument, after which the argument is smart cast to non-null
     * by the contract of the function: it compiles only if the function has the contract.
     */
    data class ContractSmartCast(val funId: Int) : Use

    /** A smart cast of the nullable top-level property [propId] to non-null, which compiles only while the property is stable. */
    data class SmartCast(val propId: Int) : Use

    /** A smart cast of the nullable property [memberId] of the object [objectId] to non-null, as [SmartCast]. */
    data class SmartCastMember(val objectId: Int, val memberId: Int) : Use

    /** A `when` with a branch condition reading the property [memberId] of the object [objectId], inlined when it is a constant. */
    data class WhenMember(val objectId: Int, val memberId: Int) : Use

    /**
     * A destructuring declaration of an instance of the data class [classId]: by position into [componentCount] variables,
     * or by the name of the property [propertyName], which stops compiling when the property is renamed.
     */
    data class Destructure(val classId: Int, val componentCount: Int, val propertyName: String?) : Use

    /** A `copy()` call on an instance of the data class [classId]. */
    data class Copy(val classId: Int) : Use

    /** A read of the constructor property [name] of an instance of [classId], which stops compiling when it is removed or renamed. */
    data class ReadCtorProperty(val classId: Int, val name: String) : Use

    /**
     * A reference to the function [funId] assigned to a variable of the function type the function had when the use was created:
     * [paramTypes] and [returnType], with its receiver first. The reference resolves to the overload of this type.
     */
    data class FunRef(val funId: Int, val paramTypes: List<TypeRef>, val returnType: TypeRef) : Use

    /** A reference to the top-level property [propId] assigned to a variable of the property type of the [type] it had. */
    data class PropRef(val propId: Int, val type: TypeRef) : Use

    /** A reference to the member [memberId] of [classId], as [FunRef]: a function reference with the receiver first, or a property one. */
    data class MemberRef(val classId: Int, val memberId: Int, val paramTypes: List<TypeRef>, val returnType: TypeRef, val isProperty: Boolean) : Use

    /** A class literal of [classId]. */
    data class ClassRef(val classId: Int) : Use

    /** An `is` check of an instance of [valueClassId] against [checkedClassId], which compiles only while the classes are related. */
    data class IsCheck(val valueClassId: Int, val checkedClassId: Int) : Use

    /** A safe cast of an instance of [valueClassId] to [checkedClassId]. */
    data class SafeCast(val valueClassId: Int, val checkedClassId: Int) : Use

    /** A call of the member [memberId] on the result of [call], with a safe call if [safe]: a regular one stops compiling when the result becomes nullable. */
    data class ChainedCall(val call: CallFun, val memberId: Int, val safe: Boolean) : Use

    /** The result of [call] with an elvis operator. */
    data class Elvis(val call: CallFun) : Use

    /** An assignment of the `var` top-level property [propId], which stops compiling when it becomes a `val` or its setter becomes private. */
    data class WriteProp(val propId: Int) : Use

    /** An assignment of the `var` member property [memberId] of an instance of [classId], as [WriteProp]. */
    data class WriteMember(val classId: Int, val memberId: Int) : Use

    /** A call of the member [memberName] of the object [objectId] imported by its name, `import p.Obj.member`. */
    data class ImportedMemberCall(val objectId: Int, val memberId: Int, val memberName: String) : Use

    /** A read of the [entry] of the enum [enumId] imported by its name, `import p.E.entry`. */
    data class ImportedEnumEntry(val enumId: Int, val entry: String) : Use
}

/**
 * The syntax of a call of an extension function. A call in the infix or in the operator form stops compiling when the function
 * loses its modifier.
 */
enum class CallForm { REGULAR, INFIX, OPERATOR }

/**
 * The operator an extension function is, with the name it gets. A `provideDelegate` operator provides a read-only property
 * with the value of its return type.
 */
enum class OperatorKind(val functionName: String) {
    PLUS("plus"), GET("get"), INVOKE("invoke"), COMPARE_TO("compareTo"), COMPONENT1("component1"), GET_VALUE("getValue"), SET_VALUE("setValue"),
    PROVIDE_DELEGATE("provideDelegate");

    /** The operators a member function may be, the delegation ones are only extensions. */
    val isMemberOperator: Boolean get() = this != GET_VALUE && this != SET_VALUE && this != PROVIDE_DELEGATE
}

sealed interface Decl {
    val id: Int
    val name: String
    val visibility: Visibility

    fun withVisibility(visibility: Visibility): Decl = when (this) {
        is FunDecl -> copy(visibility = visibility)
        is PropDecl -> copy(visibility = visibility)
        is TypeAliasDecl -> copy(visibility = visibility)
        is ClassDecl -> copy(visibility = visibility)
    }

    fun withName(name: String): Decl = when (this) {
        is FunDecl -> copy(name = name)
        is PropDecl -> copy(name = name)
        is TypeAliasDecl -> copy(name = name)
        is ClassDecl -> copy(name = name)
    }
}

/**
 * A top-level function. A call passes only the parameters without defaults and the [lambda], if any.
 *
 * @property lambda the kind of the trailing `block: () -> Int` parameter, `null` if there is none
 * @property deadBranchVariant if not 0, the body prints a string in a branch which is never taken,
 *   so changing it changes the bytecode but not the behaviour
 * @property ownedObjectVariant if not 0, the body creates an anonymous object returning this variant from `toString()`;
 *   in an inline function its class is copied to the call sites
 * @property implicitReturnType whether the function has an expression body with an inferred return type,
 *   so that a change of the body may change its signature
 * @property deprecationConstId the constant whose value is the message of the `@Deprecated` annotation of the function
 * @property receiverClassId the receiver of an extension function
 * @property infix whether the function is infix, printed only for an extension function with a single parameter
 * @property operator whether the function is the [operatorKind] operator, printed only for an extension function of its shape
 * @property operatorKind the operator, a `getValue` operator ignores the parameters of the function
 * @property jvmNameVariant if not 0, the function is renamed in the bytecode with `@JvmName`
 * @property jvmOverloads whether the function is `@JvmOverloads`, printed only for a non-inline function with default values
 * @property publishedApi whether the function is `@PublishedApi`, printed only for an internal function
 * @property hidden whether the function is deprecated with the `HIDDEN` level, so its calls do not compile
 * @property contract whether the function has a contract implying that its first parameter is not null,
 *   printed only for a non-suspend non-generic function whose first parameter is nullable
 * @property typeParam the kind of the type parameter `T`, `null` if the function is not generic. It is reified only if the function is inline.
 * @property typeParamNonNull whether `T` has the `Any` upper bound
 * @property lambdaShape the type of the [lambda] parameter
 * @property localClassVariant if not 0, a non-inline function declares and uses a local class returning this variant from `toString()`
 * @property localFunVariant if not 0, a non-inline function declares and calls a local function returning this variant
 * @property jvmSynthetic whether the function is `@JvmSynthetic`
 * @property throws whether the function declares `@Throws`
 * @property annotations the annotations with the annotation classes of the program
 * @property optInIds the opt-in markers the function opts in to with `@OptIn`
 * @property contextClassId the type of the context parameter, a call needs an instance of it as an implicit receiver
 * @property typeParam2 whether a generic function has a second type parameter `U`, which is `U : T` with [whereClause].
 *   The calls pass the same type argument for both
 * @property deprecationError whether the function is deprecated with the `ERROR` level, so its calls do not compile
 * @property replaceWith whether the deprecation has a `ReplaceWith`
 * @property tailrec whether the function is `tailrec`
 * @property synchronized whether the function is `@Synchronized`
 * @property jvmSuppressWildcards whether the function is `@JvmSuppressWildcards`
 * @property stdlibShadow the function of the standard library this function shadows in its package, printed instead of the function
 * @property jvmExposeBoxed whether the function is `@JvmExposeBoxed`, printed only where it is allowed
 * @property external whether the function is `external`, so it has no body, printed only for a function allowed to have none
 */
data class FunDecl(
    override val id: Int,
    override val name: String,
    override val visibility: Visibility,
    val inline: Boolean,
    val params: List<Param>,
    val returnType: TypeRef,
    val bodyVariant: Int,
    val uses: List<Use>,
    val suspend: Boolean = false,
    val lambda: LambdaKind? = null,
    val deadBranchVariant: Int = 0,
    val ownedObjectVariant: Int = 0,
    val implicitReturnType: Boolean = false,
    val deprecationConstId: Int? = null,
    val receiverClassId: Int? = null,
    val infix: Boolean = false,
    val operator: Boolean = false,
    val jvmNameVariant: Int = 0,
    val jvmOverloads: Boolean = false,
    val publishedApi: Boolean = false,
    val hidden: Boolean = false,
    val contract: Boolean = false,
    val typeParam: TypeParamKind? = null,
    val typeParamNonNull: Boolean = false,
    val lambdaShape: LambdaShape = LambdaShape.NO_PARAMS,
    val localClassVariant: Int = 0,
    val localFunVariant: Int = 0,
    val jvmSynthetic: Boolean = false,
    val throws: Boolean = false,
    val annotations: List<AnnotationUse> = emptyList(),
    val optInIds: List<Int> = emptyList(),
    val operatorKind: OperatorKind = OperatorKind.PLUS,
    val contextClassId: Int? = null,
    val typeParam2: Boolean = false,
    val whereClause: Boolean = false,
    val deprecationError: Boolean = false,
    val replaceWith: Boolean = false,
    val tailrec: Boolean = false,
    val synchronized: Boolean = false,
    val jvmSuppressWildcards: Boolean = false,
    val stdlibShadow: StdlibShadow? = null,
    val jvmExposeBoxed: Boolean = false,
    val external: Boolean = false,
) : Decl {
    /** Whether the function is printed as suspend: suspend inline functions are generated only with [KnownIssues.inlineOfSeparateCompilation]. */
    val isSuspend: Boolean get() = suspend && (!inline || KnownIssues.inlineOfSeparateCompilation)

    /**
     * Whether [param] is printed with its default value. The default value of a class type depends on the class, so it may change
     * without a change of the function: an inline function has such defaults only with [KnownIssues.inlineDefaultValueChange].
     */
    fun printsDefault(param: Param): Boolean =
        param.hasDefault && !param.vararg && param.type !is TypeRef.TypeParamType &&
                (!inline || KnownIssues.inlineDefaultValueChange || param.type is TypeRef.BuiltinType)

    /** Whether the function may be infix or an operator: an extension function with a single parameter without a default value. */
    val isBinary: Boolean get() = receiverClassId != null && lambda == null && params.size == 1 && !params.single().hasDefault && !params.single().vararg

    /** Whether the function is printed as the [operatorKind] operator: it has to have the shape of the operator. */
    val isOperator: Boolean
        get() = operator && receiverClassId != null && typeParam == null && when (operatorKind) {
            OperatorKind.PLUS, OperatorKind.GET -> isBinary
            OperatorKind.COMPARE_TO -> isBinary && returnType == TypeRef.BuiltinType(Builtin.INT)
            OperatorKind.INVOKE -> true
            OperatorKind.COMPONENT1 -> params.isEmpty() && lambda == null
            OperatorKind.GET_VALUE, OperatorKind.SET_VALUE, OperatorKind.PROVIDE_DELEGATE -> lambda == null
        }

    /**
     * Whether the [contract] is printed: the parameter of a function type of an inline function is an inline lambda, which it cannot use
     * (a typealias may expand to a function type), and a vararg parameter is an array.
     */
    val hasContract: Boolean
        get() = contract && !isSuspend && typeParam == null && params.firstOrNull()?.takeIf { !it.vararg }?.type
            ?.let { it.nullable && !(inline && (it is TypeRef.FunctionType || it is TypeRef.AliasType)) } == true

    val isReified: Boolean get() = inline && typeParam == TypeParamKind.REIFIED

    /** The printed [lambdaShape]: a suspend lambda of an inline function needs the function to be suspend. */
    val printedLambdaShape: LambdaShape
        get() = lambdaShape.takeIf { it != LambdaShape.SUSPEND || !inline || isSuspend } ?: LambdaShape.NO_PARAMS
}

enum class TypeParamKind { PLAIN, REIFIED }

/** The kind of a lambda parameter. The modifiers are printed only for inline functions, where they are allowed. */
enum class LambdaKind { PLAIN, NOINLINE, CROSSINLINE }

/** The type of a lambda parameter, all of them accept the `{ <Int> }` lambda. */
enum class LambdaShape(val type: String) {
    NO_PARAMS("() -> Int"), PARAM("(Int) -> Int"), RECEIVER("Int.() -> Int"), SUSPEND("suspend () -> Int")
}

/** A `lateinit` property is printed as a `var` with a value if its type is not allowed. */
enum class PropKind { VAL, VAR, CONST, LATEINIT }

/**
 * How the value of a property is provided. A getter is printed instead of a field and of a delegate for an extension property,
 * and a field is printed instead of a delegate for a `var`.
 */
enum class PropAccessor { FIELD, GETTER, INLINE_GETTER, LAZY, DELEGATE }

/**
 * @property receiverClassId the receiver of an extension property
 * @property jvmField whether the property is `@JvmField`, printed only for a property with a field
 * @property volatile whether the property is `@Volatile`, printed only for a `var` with a field
 * @property annotations the annotations with the annotation classes of the program
 * @property delegateFunId the `getValue` operator providing the value of a [PropAccessor.DELEGATE] property, which is delegated to
 *   an instance of its receiver; a `var` also needs a `setValue` operator with the same receiver. A `val` may be delegated
 *   to a `provideDelegate` operator instead
 * @property setterVisibility the visibility of the setter of a `var`, printed only for a more restrictive one
 * @property setterBodyVariant if not 0, the setter of a `var` has a body printing this variant
 * @property accessorJvmNameVariant if not 0, the accessors are renamed in the bytecode with `@get:JvmName` and `@set:JvmName`
 */
data class PropDecl(
    override val id: Int,
    override val name: String,
    override val visibility: Visibility,
    val kind: PropKind,
    val type: TypeRef,
    val valueVariant: Int,
    val implicitType: Boolean = false,
    val accessor: PropAccessor = PropAccessor.FIELD,
    val receiverClassId: Int? = null,
    val jvmField: Boolean = false,
    val volatile: Boolean = false,
    val annotations: List<AnnotationUse> = emptyList(),
    val delegateFunId: Int? = null,
    val setterVisibility: Visibility? = null,
    val setterBodyVariant: Int = 0,
    val accessorJvmNameVariant: Int = 0,
) : Decl {
    /** The printed receiver: a constant has none. */
    val printedReceiverId: Int? get() = receiverClassId.takeIf { kind != PropKind.CONST }
}

/**
 * @property generic whether the typealias has the type parameter `T`, which [target] may use
 * @property outerId the class the typealias is nested in, then it is printed in that class instead of its file
 */
data class TypeAliasDecl(
    override val id: Int,
    override val name: String,
    override val visibility: Visibility,
    val target: TypeRef,
    val generic: Boolean = false,
    val outerId: Int? = null,
) : Decl

enum class ClassKind {
    FINAL, OPEN, ABSTRACT, SEALED, INTERFACE, SEALED_INTERFACE, DATA, OBJECT, DATA_OBJECT, ENUM, VALUE, ANNOTATION;

    /** Whether the class can be a supertype in the model. */
    val isInheritable: Boolean get() = this == OPEN || this == ABSTRACT || this == SEALED || isInterface

    /** Whether the class may declare abstract members. */
    val allowsAbstractMembers: Boolean get() = this == ABSTRACT || this == SEALED || isInterface

    val isInterface: Boolean get() = this == INTERFACE || this == SEALED_INTERFACE

    val isSealed: Boolean get() = this == SEALED || this == SEALED_INTERFACE

    val isObject: Boolean get() = this == OBJECT || this == DATA_OBJECT

    /** Whether the class may be an inner class or have a companion object. */
    val isRegularClass: Boolean get() = this == FINAL || this == OPEN || this == ABSTRACT || this == SEALED || this == DATA

    /** Whether the class has a primary constructor with the parameters of the model. */
    val hasConstructor: Boolean get() = isRegularClass
}

enum class Modality { FINAL, OPEN, ABSTRACT }

/** How a class is declared in its [ClassDecl.outerId]. */
enum class Nesting { NESTED, INNER, COMPANION }

/** The declarations an annotation class may annotate. */
enum class AnnotationTargets { ALL, CLASSES, FUNCTIONS, TYPES }

enum class Variance(val keyword: String) { INVARIANT(""), OUT("out "), IN("in ") }

/**
 * @property interfaceIds the interfaces implemented in addition to the supertype [superId]
 * @property outerId the class this class is declared in, then it is printed in that class instead of its file
 * @property enumConstructorVariant if not 0, the enum class has a constructor parameter passed by its entries
 * @property enumEntryBodyVariant if not 0, the enum entries have bodies
 * @property funInterface whether an interface is a `fun interface`, printed only if it has a single abstract member function
 * @property ctorParams the parameters of the primary constructor, a data class also has the `v` property
 * @property secondaryCtor whether the class has a secondary constructor with `String` and `Unit` parameters delegating to the primary one
 * @property initVariant if not 0, the class has an `init` block printing this variant
 * @property ctorVisibility the visibility of the primary constructor
 * @property delegatedIds the interfaces of [interfaceIds] implemented by delegation to a constructor parameter
 * @property retention the retention of an annotation class
 * @property annotationTargets the targets of an annotation class
 * @property requiresOptIn whether an annotation class is a `@RequiresOptIn` marker
 * @property annotations the annotations of the class with the annotation classes of the program
 * @property typeParam the variance of the type parameter `T`, `null` if the class is not generic. The members may use `T`,
 *   the overrides in the subclasses, which pass `Int` as the type argument, use `Int` instead
 * @property typeParamNonNull whether `T` has the `Any` upper bound
 * @property unnamedCompanion whether a companion object has no name, so it is referenced as `Companion`
 * @property enumAbstractMember whether an enum class has an abstract member implemented by each of its entries
 * @property repeatable whether an annotation class is `@Repeatable`
 * @property annotationParams the parameters of an annotation class in addition to `v: String`
 * @property annotationEnumId the enum class of the [AnnotationParam.ENUM] parameter of an annotation class
 * @property optInWarningLevel whether an opt-in marker has the `WARNING` level instead of the `ERROR` one
 * @property jvmRecord whether a data class is a `@JvmRecord`, printed only for one without a superclass and fields of its own
 * @property jvmExposeBoxed whether a value class is `@JvmExposeBoxed`
 * @property jvmDefaultWithoutCompatibility whether the class is `@JvmDefaultWithoutCompatibility`, so it has no `DefaultImpls`
 *   and no delegates to them
 * @property subclassOptInId the opt-in marker the subclasses of the class require with `@SubclassOptInRequired`,
 *   printed only for an open or abstract class and an interface which is not a `fun interface`
 * @property optInIds the opt-in markers the class opts in to with `@OptIn`
 * @property consistentCopyVisibility whether a data class is `@ConsistentCopyVisibility`, so its `copy()` has the visibility of its constructor
 */
data class ClassDecl(
    override val id: Int,
    override val name: String,
    override val visibility: Visibility,
    val kind: ClassKind,
    val superId: Int?,
    val members: List<MemberFun>,
    val enumEntries: List<String>,
    val interfaceIds: List<Int> = emptyList(),
    val outerId: Int? = null,
    val nesting: Nesting = Nesting.NESTED,
    val enumConstructorVariant: Int = 0,
    val enumEntryBodyVariant: Int = 0,
    val funInterface: Boolean = false,
    val ctorParams: List<CtorParam> = emptyList(),
    val secondaryCtor: Boolean = false,
    val initVariant: Int = 0,
    val ctorVisibility: Visibility = Visibility.PUBLIC,
    val delegatedIds: Set<Int> = emptySet(),
    val retention: AnnotationRetention = AnnotationRetention.RUNTIME,
    val annotationTargets: AnnotationTargets = AnnotationTargets.ALL,
    val requiresOptIn: Boolean = false,
    val annotations: List<AnnotationUse> = emptyList(),
    val typeParam: Variance? = null,
    val typeParamNonNull: Boolean = false,
    val unnamedCompanion: Boolean = false,
    val enumAbstractMember: Boolean = false,
    val repeatable: Boolean = false,
    val annotationParams: Set<AnnotationParam> = emptySet(),
    val annotationEnumId: Int? = null,
    val optInWarningLevel: Boolean = false,
    val jvmRecord: Boolean = false,
    val jvmExposeBoxed: Boolean = false,
    val jvmDefaultWithoutCompatibility: Boolean = false,
    val subclassOptInId: Int? = null,
    val optInIds: List<Int> = emptyList(),
    val consistentCopyVisibility: Boolean = false,
) : Decl {
    /** Whether the class is printed as generic: objects, enums and annotation classes cannot be. */
    val isGeneric: Boolean get() = typeParam != null && !kind.isObject && kind != ClassKind.ENUM && kind != ClassKind.ANNOTATION

    val isInstantiable: Boolean
        get() = when (kind) {
            ClassKind.FINAL, ClassKind.OPEN, ClassKind.DATA, ClassKind.OBJECT, ClassKind.DATA_OBJECT, ClassKind.VALUE -> true
            ClassKind.ENUM -> enumEntries.isNotEmpty()
            ClassKind.ABSTRACT, ClassKind.SEALED, ClassKind.INTERFACE, ClassKind.SEALED_INTERFACE, ClassKind.ANNOTATION -> false
        }

    /**
     * The printed constructor parameters: a data class has only property parameters, and the delegation to an interface
     * is to a constructor parameter.
     */
    val printedCtorParams: List<CtorParam>
        get() = if (!kind.hasConstructor) emptyList() else {
            ctorParams.map { if (kind == ClassKind.DATA) it.copy(isProperty = true) else it } +
                    printedDelegatedIds.map { CtorParam("d$it", TypeRef.ClassType(it), hasDefault = false, isProperty = kind == ClassKind.DATA) }
        }

    /** The interfaces actually implemented by delegation, only a class with a constructor delegates. */
    val printedDelegatedIds: List<Int> get() = if (kind.hasConstructor) interfaceIds.filter { it in delegatedIds } else emptyList()

    fun replaceMember(id: Int, transform: (MemberFun) -> MemberFun): ClassDecl =
        copy(members = members.map { if (it.id == id) transform(it) else it })

    /**
     * The single abstract member function of a `fun interface`, `null` if there is none or more than one abstract member,
     * or it has default values, a type parameter, an extension receiver or is suspend.
     */
    val samMember: MemberFun?
        get() = members.filter { it.modality == Modality.ABSTRACT && it.overrides == null }.singleOrNull()
            ?.takeIf { !it.isProperty && it.params.none(Param::hasDefault) && !it.typeParam && it.extensionReceiver == null && !it.suspend }

    /** Whether the interface is printed as a `fun interface`: only one without supertypes, whose abstract members are all its own. */
    val isFunInterface: Boolean
        get() = funInterface && kind == ClassKind.INTERFACE && superId == null && interfaceIds.isEmpty() && samMember != null
}

/**
 * A member function, or a member property with a getter when [isProperty].
 * An overriding member takes its signature from the root of [overrides], so it is printed consistently with the overridden member,
 * except for [covariantReturnType]: a nullable return type of the root is overridden with the non-nullable one.
 *
 * @property propKind the kind of a property, taken from the root of [overrides]. A constant is printed only in an object,
 *   and a `lateinit` property only if its type allows it
 * @property accessor how the value of a property is provided: a [PropAccessor.FIELD] with an initializer or a [PropAccessor.GETTER],
 *   an interface property always has a getter
 * @property finalOverride whether an override is `final`
 * @property overridesWithOtherKind whether an override of a function is printed as a property, or the other way round, which does not compile
 * @property inline whether the member is inline, printed only for a final member which is not an override, nor in an interface
 * @property suspend whether a member function is suspend, taken from the root of [overrides]
 * @property infix whether a member function is infix, printed only for a member function with a single parameter
 * @property operator whether a member function is the [operatorKind] operator, printed only for a member of its shape
 * @property typeParam whether a member function has the type parameter `M`, taken from the root of [overrides]
 * @property extensionReceiver the receiver of a member extension function, taken from the root of [overrides]
 * @property setterVisibility the visibility of the setter of a `var`, printed only for a more restrictive one of a final property
 * @property setterBodyVariant if not 0, the setter of a `var` has a body printing this variant
 * @property accessorJvmNameVariant if not 0, the accessors of a final property are renamed with `@get:JvmName` and `@set:JvmName`
 * @property transient whether a property with a field is `@Transient`
 * @property synchronized whether a member function with a body is `@Synchronized`
 */
data class MemberFun(
    val id: Int,
    val name: String,
    val modality: Modality,
    val overrides: Int?,
    val returnType: TypeRef,
    val bodyVariant: Int,
    val uses: List<Use>,
    val params: List<Param> = emptyList(),
    val isProperty: Boolean = false,
    val visibility: Visibility = Visibility.PUBLIC,
    val covariantReturnType: Boolean = false,
    val jvmStatic: Boolean = false,
    val propKind: PropKind = PropKind.VAL,
    val accessor: PropAccessor = PropAccessor.GETTER,
    val jvmField: Boolean = false,
    val finalOverride: Boolean = false,
    val overridesWithOtherKind: Boolean = false,
    val inline: Boolean = false,
    val suspend: Boolean = false,
    val infix: Boolean = false,
    val operator: Boolean = false,
    val operatorKind: OperatorKind = OperatorKind.PLUS,
    val typeParam: Boolean = false,
    val extensionReceiver: TypeRef? = null,
    val setterVisibility: Visibility? = null,
    val setterBodyVariant: Int = 0,
    val accessorJvmNameVariant: Int = 0,
    val transient: Boolean = false,
    val synchronized: Boolean = false,
) {
    /** Whether the member function may be infix or a binary operator: it has a single parameter without a default value. */
    val isBinary: Boolean
        get() = !isProperty && extensionReceiver == null && params.size == 1 && !params.single().hasDefault && !params.single().vararg

    /** Whether the member function is printed as the [operatorKind] operator: it has to have the shape of the operator. */
    val isOperator: Boolean
        get() = operator && !isProperty && !typeParam && extensionReceiver == null && operatorKind.isMemberOperator && when (operatorKind) {
            OperatorKind.PLUS, OperatorKind.GET -> isBinary
            OperatorKind.COMPARE_TO -> isBinary && returnType == TypeRef.BuiltinType(Builtin.INT)
            OperatorKind.INVOKE -> true
            OperatorKind.COMPONENT1 -> params.isEmpty()
            OperatorKind.GET_VALUE, OperatorKind.SET_VALUE, OperatorKind.PROVIDE_DELEGATE -> false
        }
}
