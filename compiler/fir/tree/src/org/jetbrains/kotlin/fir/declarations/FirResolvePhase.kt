/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

/**
 * Compilation in the FIR compiler is performed in separate compiler phases.
 *
 * ### Modes
 * There are two compiler modes:
 * - The CLI mode (aka full resolution mode), the compiler executes all phases sequentially for all declarations.
 *   This means that `A` phase processor will transform all declarations in the first round.
 *   In the next round, `B` phase (which follows `A`) processor will transform all declarations.
 *
 * - The Analysis API mode (aka lazy resolution mode),
 *   the compiler executes the requested phase [lazily][org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase]
 *   (on demand) for some particular declaration (not for all declarations in the opposite to the CLI mode).
 *
 * ### Contacts
 * There are a few contacts for both modes:
 * 1. All phases **must be executed only in the sequential manner**.
 *   If the compiler wants to resolve some declaration to the phase [COMPILER_REQUIRED_ANNOTATIONS],
 *   then it has to resolve it to all previous phases firstly: to the [RAW_FIR] phase,
 *   then to the phase [IMPORTS] and only after that the compiler can execute the phase [COMPILER_REQUIRED_ANNOTATIONS].
 *
 * 2. The compiler **must not request and cannot rely on the information from the higher phases**.
 *   For example, during the [STATUS] phase,
 *   we cannot have information regarding implicit types as they will be calculated only during [IMPLICIT_TYPES_BODY_RESOLVE] phase.
 *
 * 3. The compiler **can request and rely on the information from the current phase only during *jumping phases***.
 *   For example, during the [TYPES] phase,
 *   we cannot request type information for other declarations (except information from the [SUPER_TYPES] phase like a super type)
 *   as this information will be calculated only during this phase.
 *
 * ### Jumping phases
 * *Jumping phase* – it is a phase that can request the information from the same phase from another declaration.
 *
 * Currently, we have four ***jumping phases***:
 * - [COMPILER_REQUIRED_ANNOTATIONS] – The compiler can jump from the use side of an annotation to the annotation class
 *   and resolve its annotations as well.
 * - [SUPER_TYPES] – The compiler resolves super types recursively for all classes from the bottom class to the top one.
 * - [STATUS] – The compiler has to resolve the status for super declarations firstly.
 * - [IMPLICIT_TYPES_BODY_RESOLVE] – The compiler can jump from one declaration to another during this phase as one
 *   declaration with an implicit type can depend on an implicit type on another declaration,
 *   and the compiler needs to calculate the type for it firstly.
 *
 * @see org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException
 */
enum class FirResolvePhase(val noProcessor: Boolean = false) {
    /**
     * We ran the translator from some parser AST to FIR tree.
     * Currently, FIR supports two parser representations:
     * - Program Structure Interface ([PSI][com.intellij.psi.PsiElement])
     * - Light Tree ([LT][com.intellij.lang.LighterASTNode])
     *
     * During conversion, the FIR translator performs desugaring of the source code.
     * This includes replacing all `if` expressions with corresponding `when` expressions,
     * converting `for` loops into blocks with `iterator` variable declaration and `while` loop, and similar.
     */
    RAW_FIR(noProcessor = true),

    /**
     * The compiler resolves all imports in the file.
     * Effectively, it means that the compiler splits all imports on longest existing package part and related class qualifier.
     * More specifically, if an import is `import aaa.bbb.ccc.D` then the compiler tries to resolve `aaa.bbb.ccc` package firstly
     * and if it is not found then trying to find `aaa.bbb` package.
     */
    IMPORTS,

    /**
     * The compiler resolves types of some specific compiler annotations (like [SinceKotlin] or [JvmRecord])
     * which are required earlier than [TYPES] phase or other annotations which are required for some compiler plugins.
     * For some annotations (like [Deprecated] or [Target]) not only the type, but also the arguments are resolved.
     *
     * Also, calculates [DeprecationsProvider].
     *
     * This is a [*jumping phase*][FirResolvePhase].
     */
    COMPILER_REQUIRED_ANNOTATIONS,

    /**
     * The compiler generates companion objects which were provided by compiler plugins.
     */
    COMPANION_GENERATION,

    /**
     * The compiler resolves all supertypes of classes and performs type aliases expansion.
     *
     * This is a [*jumping phase*][FirResolvePhase].
     */
    SUPER_TYPES,

    /**
     * The compiler collects and records all inheritors of sealed classes.
     */
    SEALED_CLASS_INHERITORS,

    /**
     * The compiler resolves all other explicitly written types in declaration headers including
     * - explicit return type for [callables][FirCallableDeclaration]
     * - value parameters type for [functions][FirFunction]
     * - property accessors type for [variables][FirVariable]
     * - backing field type for [variables][FirVariable]
     * - extension receivers type for [callables][FirCallableDeclaration]
     * - context receivers type for [callables][FirCallableDeclaration] and [classes][FirRegularClass]
     * - bounds types for [type parameters][FirTypeParameter]
     * - types of annotations (without resolution of annotation arguments)
     * for [annotation containers][org.jetbrains.kotlin.fir.FirAnnotationContainer].
     *
     * @see IMPLICIT_TYPES_BODY_RESOLVE
     */
    TYPES,

    /**
     * The compiler resolves modality, visibility, and modifiers for [member declarations][FirMemberDeclaration].
     * Note: member's modality and modifiers may depend on super declarations in the case when "this"
     * member overrides some other member.
     *
     * This is a [*jumping phase*][FirResolvePhase].
     */
    STATUS,

    /**
     * The compiler matches and records an `expect` member declaration for `actual` [member declarations][FirMemberDeclaration].
     */
    EXPECT_ACTUAL_MATCHING,

    /**
     * The compiler resolves a [contract][org.jetbrains.kotlin.fir.contracts.FirContractDescription] definition in [contract owners][FirContractDescriptionOwner]:
     * - [property accessors][FirPropertyAccessor]
     * - [functions][FirSimpleFunction]
     * - [constructors][FirConstructor]
     */
    CONTRACTS,

    /**
     * The compiler resolves types for [callable declarations][FirCallableDeclaration] without an explicit return type.
     * Examples:
     * ```kotlin
     * fun foo() = 0 // implicit type is Int
     * val bar = "str" // implicit type is String
     * val baz get() = foo() // implicit type is Int
     * ```
     *
     * This is a [*jumping phase*][FirResolvePhase].
     *
     * @see TYPES
     */
    IMPLICIT_TYPES_BODY_RESOLVE,

    /**
     * The compiler resolves arguments of annotations in declaration headers.
     */
    ANNOTATION_ARGUMENTS,

    /**
     * The compiler resolves bodies of declarations including
     * - bodies for [functions][FirFunction]
     * - bodies for [anonymous initializers][FirAnonymousInitializer]
     * - initializers for [variables][FirVariable]
     * - delegates for [variables][FirVariable]
     * - default values for [value parameters][FirValueParameter]
     *
     * Also, during this resolution,
     * the compiler calculates [control flow graph][org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference]
     * for declaration which [own][FirControlFlowGraphOwner] it.
     */
    BODY_RESOLVE;

    val next: FirResolvePhase get() = values()[ordinal + 1]
    val previous: FirResolvePhase get() = values()[ordinal - 1]

    companion object {
        // Short-cut
        val DECLARATIONS = STATUS
        val ANALYZED_DEPENDENCIES = BODY_RESOLVE
    }
}

val FirResolvePhase.isBodyResolve: Boolean
    get() = when (this) {
        FirResolvePhase.BODY_RESOLVE,
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> true
        else -> false
    }
