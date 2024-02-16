/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

/**
 * Compilation in the FIR compiler is performed in separate compiler phases.
 *
 * ### Modes
 *
 * There are two compiler modes:
 *
 * - The CLI compiler mode (aka full resolution mode): the compiler executes all phases sequentially for all declarations.
 *   This means that the processor for phase `A` will transform all declarations in the first round.
 *   In the next round, the processor for phase `B` (which follows `A`) will transform all declarations.
 *
 * - The Analysis API mode (aka lazy resolution mode):
 *   the compiler executes the requested phase [lazily][org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase]
 *   (on demand) for some particular declaration (not for all declarations in contrast to the CLI mode).
 *
 * ### Contracts
 *
 * There are a few contracts for both modes:
 *
 * 1. All phases **must be executed only in a sequential manner**.
 *   For example, if the compiler wants to resolve some declaration to the phase [COMPILER_REQUIRED_ANNOTATIONS],
 *   then it has to resolve it to all previous phases first: to the [RAW_FIR] phase,
 *   then to the [IMPORTS] phase, and only afterward can the compiler execute the phase [COMPILER_REQUIRED_ANNOTATIONS].
 *
 * 2. The compiler **must not request and cannot rely on any information from the higher phases**.
 *   For example, during the [STATUS] phase, we cannot access information regarding implicit types
 *   as they will only be calculated during the [IMPLICIT_TYPES_BODY_RESOLVE] phase.
 *
 * 3. The compiler **can request and rely on the information from the current phase only during *jumping phases***.
 *   For example, during the [TYPES] phase,
 *   we cannot request type information for other declarations (except information from the [SUPER_TYPES] phase, such as a super type)
 *   as this information will be calculated only during this phase.
 *
 * ### Jumping phases
 *
 * A *jumping phase* is a phase that can request the phase-specific information during that same phase from another declaration.
 *
 * Currently, we have four ***jumping phases***:
 *
 * - [COMPILER_REQUIRED_ANNOTATIONS] – The compiler can jump from the use site of an annotation to the annotation class
 *   and resolve its annotations as well.
 * - [SUPER_TYPES] – The compiler resolves super types recursively for all classes from the bottom class to the top one.
 * - [STATUS] – The compiler resolves the status for super declarations first.
 * - [IMPLICIT_TYPES_BODY_RESOLVE] – The compiler can jump from one declaration to another during this phase as one
 *   declaration with an implicit type can depend on the implicit type of another declaration,
 *   and the compiler needs to calculate the type for it first.
 *
 * @see org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException
 */
enum class FirResolvePhase(val noProcessor: Boolean = false) {
    /**
     * We ran the translator from some parser AST to FIR tree.
     *
     * Currently, FIR supports two parser representations:
     * - Program Structure Interface ([PSI][com.intellij.psi.PsiElement])
     * - Light Tree ([LT][com.intellij.lang.LighterASTNode])
     *
     * During conversion, the FIR translator performs desugaring of the source code.
     * This includes replacing all `if` expressions with corresponding `when` expressions,
     * converting `for` loops into blocks with an `iterator` variable declaration and a `while` loop, and similar.
     */
    RAW_FIR(noProcessor = true),

    /**
     * The compiler resolves all imports in the file.
     *
     * Effectively, this means that the compiler splits all imports on the longest existing package part and the related class qualifier.
     * More specifically, if an import is `import aaa.bbb.ccc.D`, the compiler tries to resolve the package `aaa.bbb.ccc` first.
     * If it doesn't exist, it tries to find the package `aaa.bbb`, and so on. The result is a pair of package name and class qualifier
     * which exactly identifies the imported declaration.
     */
    IMPORTS,

    /**
     * The compiler resolves types of some specific compiler annotations (like [SinceKotlin] or [JvmRecord])
     * which are required earlier than the [TYPES] phase or other annotations which are required for some compiler plugins.
     * For some annotations (like [Deprecated] or [Target]) not only the type, but also the arguments are resolved.
     *
     * Also calculates [DeprecationsProvider].
     *
     * This is a [*jumping phase*][FirResolvePhase].
     */
    COMPILER_REQUIRED_ANNOTATIONS,

    /**
     * The compiler generates companion objects which were provided by compiler plugins.
     */
    COMPANION_GENERATION,

    /**
     * The compiler resolves all supertypes of classes and performs type alias expansion.
     *
     * This is a [*jumping phase*][FirResolvePhase].
     */
    SUPER_TYPES,

    /**
     * The compiler collects and records all inheritors of sealed classes.
     */
    SEALED_CLASS_INHERITORS,

    /**
     * The compiler resolves all other explicitly written types in declaration headers, including:
     *
     * - explicit return type for [callables][FirCallableDeclaration]
     * - value parameter types for [functions][FirFunction]
     * - property accessor types for [variables][FirVariable]
     * - backing field type for [variables][FirVariable]
     * - extension receiver type for [callables][FirCallableDeclaration]
     * - context receiver types for [callables][FirCallableDeclaration] and [classes][FirRegularClass]
     * - type bounds for [type parameters][FirTypeParameter]
     * - types of annotations (without resolution of annotation arguments)
     *   for [annotation containers][org.jetbrains.kotlin.fir.FirAnnotationContainer].
     *
     * @see IMPLICIT_TYPES_BODY_RESOLVE
     */
    TYPES,

    /**
     * The compiler resolves modality, visibility, and modifiers for [member declarations][FirMemberDeclaration].
     *
     * Note: A member's modality and its modifiers may depend on super declarations when "this" member overrides some other member.
     *
     * This is a [*jumping phase*][FirResolvePhase].
     */
    STATUS,

    /**
     * The compiler matches and records an `expect` member declaration for each `actual` [member declaration][FirMemberDeclaration].
     */
    EXPECT_ACTUAL_MATCHING,

    /**
     * The compiler resolves a [contract][org.jetbrains.kotlin.fir.contracts.FirContractDescription] definition in [contract owners][FirContractDescriptionOwner]:
     *
     * - [property accessors][FirPropertyAccessor]
     * - [functions][FirSimpleFunction]
     * - [constructors][FirConstructor]
     */
    CONTRACTS,

    /**
     * The compiler resolves types for [callable declarations][FirCallableDeclaration] without an explicit return type.
     *
     * Examples:
     *
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
     * The compiler resolves bodies of declarations, including:
     *
     * - bodies for [functions][FirFunction]
     * - bodies for [anonymous initializers][FirAnonymousInitializer]
     * - initializers for [variables][FirVariable]
     * - delegates for [variables][FirVariable]
     * - default values for [value parameters][FirValueParameter]
     *
     * Also, during this resolution,
     * the compiler calculates the [control flow graph][org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference]
     * for its [owner][FirControlFlowGraphOwner] declaration.
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
