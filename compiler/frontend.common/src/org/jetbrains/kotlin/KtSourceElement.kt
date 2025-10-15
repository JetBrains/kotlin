/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SuspiciousFakeSourceCheck::class)

package org.jetbrains.kotlin

import com.intellij.lang.LighterASTNode
import com.intellij.lang.TreeBackedLighterAST
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.getElementTextWithContext
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

sealed class KtSourceElementKind {
    abstract val shouldSkipErrorTypeReporting: Boolean
}

object KtRealSourceElementKind : KtSourceElementKind() {
    override val shouldSkipErrorTypeReporting: Boolean
        get() = false
}

/**
 * When an element has a kind of [KtFakeSourceElementKind] it means that the relevant FIR element was created synthetically. And while this
 * definition might look a bit vague because, for example, the raw FIR builder might create a lot of "synthetic" things and we don't want to
 * treat all of them as "fake" (like `when`s created from `if`s), there is a criteria that ultimately means that one needs to use
 * [KtFakeSourceElementKind], and it's the situation when several FIR elements might share the same real source element.
 *
 * And vice versa, [KtRealSourceElementKind] means that there's a single FIR node in the resulting tree that has the same source element.
 *
 * ### Distinct Source Elements
 *
 * **Contract:** To support unambiguous source-based FIR symbol IDs, each pair of `(realSource, fakeElementKind)` must be unique. This
 * allows source elements for different declarations to be distinct, which supports the equality that's required by symbol IDs. In contrast,
 * if two source elements for different declarations were equal, their source-based symbol IDs would also be unexpectedly equal.
 *
 * Such pair-wise uniqueness is easier to support than it sounds if we consider the following: Since fake source elements are still bound to
 * a real element, there is a boundary where two fake source elements cannot be equal because they have different real elements. The problem
 * of fake source element equality can then be solved locally, as the creation of fake source elements from a specific real element is
 * localized to a single place (in the data). Each synthetic declaration generated from the real element simply needs to have its own fake
 * source element kind.
 *
 * TODO (marco): Also link this contract in [KtSourceElement].
 */
sealed class KtFakeSourceElementKind(final override val shouldSkipErrorTypeReporting: Boolean = false) : KtSourceElementKind() {
    /**
     * for some fir expression implicit return typeRef is generated
     * some of them are: break, continue, return, throw, string concat,
     * destruction parameters, function literals, explicitly boolean expressions
     */
    object ImplicitTypeRef : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * for errors on smartcast types then the type is brought in by an implicit this receiver expression
     */
    object ImplicitThisReceiverExpression : KtFakeSourceElementKind()

    /**
     * for implicit context parameter arguments of calls.
     */
    object ImplicitContextParameterArgument : KtFakeSourceElementKind()

    /**
     * for type arguments that were inferred as opposed to specified
     * explicitly via `<>`
     */
    object ImplicitTypeArgument : KtFakeSourceElementKind()

    /**
     * for ConeErrorTypes seen through a typealias expansion
     */
    object ErroneousTypealiasExpansion : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * for return types of anonymous functions, because ImplicitTypeRef
     * may sometimes hide the diagnostic turning red code into green
     */
    object ImplicitFunctionReturnType : KtFakeSourceElementKind()

    /**
     * for each class special class self type ref is created
     * and have a fake source referencing it
     */
    object ClassSelfTypeRef : KtFakeSourceElementKind()

    /**
     * FirErrorTypeRef may be built using unresolved firExpression
     * and have a fake source referencing it
     */
    object ErrorTypeRef : KtFakeSourceElementKind()

    /**
     * for properties without accessors default getter & setter are generated
     * they have a fake source which refers to property
     */
    object DefaultAccessor : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * for delegated properties, getter & setter calls to the delegate
     * they have a fake source which refers to the call that creates the delegate
     */
    object DelegatedPropertyAccessor : KtFakeSourceElementKind()

    /**
     * for kt classes without implicit primary constructor one is generated
     * with a fake source which refers to containing class
     */
    object ImplicitConstructor : KtFakeSourceElementKind()

    /**
     * for constructor type parameters, because they refer to the same source
     * as the class type parameters themselves
     */
    object ConstructorTypeParameter : KtFakeSourceElementKind()

    /**
     * for constructors which do not have delegated constructor call the fake one is generated
     * with a fake sources which refers to the original constructor
     */
    object DelegatingConstructorCall : KtFakeSourceElementKind()

    /**
     * for enum entry with bodies the initializer in a form of anonymous object is generated
     * with a fake sources which refers to the enum entry
     */
    object EnumInitializer : KtFakeSourceElementKind()

    /**
     *  for lambdas with implicit return the return statement is generated which is labeled
     *  with a fake sources which refers to the target expression
     */
    object GeneratedLambdaLabel : KtFakeSourceElementKind()

    /**
     * for error element which is created for dangling modifier lists
     */
    object DanglingModifierList : KtFakeSourceElementKind()

    /** for lambdas & functions with expression bodies the return statement is added
     * with a fake sources which refers to the return target
     */
    sealed class ImplicitReturn : KtFakeSourceElementKind() {
        object FromExpressionBody : ImplicitReturn()

        object FromLastStatement : ImplicitReturn()
    }

    sealed class ImplicitUnit : KtFakeSourceElementKind() {
        /** this source is used for implicit returns from empty lambdas {}
         * fake source refers to the lambda expression
         */
        object ForEmptyLambda : ImplicitUnit()

        /** this source is used for 'return' without given value converted to 'return Unit'
         * fake source refers to the return statement
         */
        object Return : ImplicitUnit()

        /** this source is used for `a[i] = b` or `a[i] += b` converted to `{ a[i] = b; Unit }`
         * fake source refers to the assignment statement
         */
        object IndexedAssignmentCoercion : ImplicitUnit()
    }

    /**
     * delegates are wrapped into FirWrappedDelegateExpression
     * with a fake sources which refers to delegated expression
     */
    object WrappedDelegate : KtFakeSourceElementKind()

    /**
     *  `for (i in list) { println(i) }` is converted to
     *  ```
     *  val <iterator>: = list.iterator()
     *  while(<iterator>.hasNext()) {
     *    val i = <iterator>.next()
     *    println(i)
     *  }
     *  ```
     *  where the generated WHILE loop has source element of initial FOR loop,
     *  other generated elements are marked as fake ones
     */
    object DesugaredForLoop : KtFakeSourceElementKind()

    object ImplicitInvokeCall : KtFakeSourceElementKind()

    /**
     * Consider an atomic qualified access like `i`. In the FIR tree, both the FirQualifiedAccessExpression and its calleeReference uses
     * `i` as the source. Hence, this fake kind is set on the `calleeReference` to make sure no PSI element is shared by multiple FIR
     * elements. This also applies to `this` and `super` references.
     */
    object ReferenceInAtomicQualifiedAccess : KtFakeSourceElementKind()

    /**
     * For enum classes, FIR generates the `valueOf()` and `values()` functions, which are covered by the various fake source element kinds
     * defined in this class.
     *
     * The fake element kinds must be kept in sync with [ALL_ENUM_GENERATED_DECLARATIONS].
     */
    sealed class EnumGeneratedDeclaration : KtFakeSourceElementKind() {
        /**
         * The source kind of an enum class's `values()` function. Its source is the corresponding enum class.
         */
        object EnumValuesFunction : EnumGeneratedDeclaration()

        /**
         * The source kind of a return type reference of the generated [EnumValuesFunction]. Its source is the corresponding enum class.
         */
        object EnumValuesFunctionReturnType : EnumGeneratedDeclaration()

        /**
         * The source kind of an enum class's `valueOf()` function. Its source is the corresponding enum class.
         */
        object EnumValueOfFunction : EnumGeneratedDeclaration()

        /**
         * The source kind of the parameter of the generated [EnumValueOfFunction]. Its source is the corresponding enum class.
         */
        object EnumValueOfFunctionParameter : EnumGeneratedDeclaration()

        /**
         * The source kind of the return type reference of the generated [EnumValueOfFunction]. Its source is the corresponding enum class.
         */
        object EnumValueOfFunctionReturnType : EnumGeneratedDeclaration()

        /**
         * The source kind of an enum class's `entries` property. Its source is the corresponding enum class.
         */
        object EnumEntriesProperty : EnumGeneratedDeclaration()

        /**
         * The source kind of the return type of the [EnumEntriesProperty]. Its source is the corresponding enum class.
         */
        object EnumEntriesPropertyReturnType : EnumGeneratedDeclaration()

        /**
         * The source kind of the getter of the [EnumEntriesProperty]. Its source is the corresponding enum class.
         */
        object EnumEntriesPropertyGetter : EnumGeneratedDeclaration()

        /**
         * The source kind of the return type of the [EnumEntriesPropertyGetter]. Its source is the corresponding enum class.
         */
        object EnumEntriesPropertyGetterReturnType : EnumGeneratedDeclaration()

        /**
         * The source kind of the enum class's `clone()` function, which may be injected on non-JVM platforms. Its source is the
         * corresponding enum class.
         */
        object EnumCloneFunction : EnumGeneratedDeclaration()
    }

    /**
     * for enum classes we can have an implicit supertype ref to `Enum` with a fake source.
     */
    object EnumSuperTypeRef : KtFakeSourceElementKind()

    /**
     * for record classes we can have an implicit supertype ref to `Record` with a fake source.
     */
    object RecordSuperTypeRef : KtFakeSourceElementKind()

    /**
     * `when (x) { "abc" -> 42 }` --> `when(val $subj = x) { $subj == "abc" -> 42 }`
     * where `$subj == "42"` has fake psi source which refers to "42" as inner expression
     * and `$subj` fake source refers to "42" as `KtWhenCondition`.
     */
    object WhenCondition : KtFakeSourceElementKind()

    /**
     * for additional FIR built for code fragments
     */
    object CodeFragment : KtFakeSourceElementKind()

    /**
     * `when { is Int -> 42 }` --> `when { $subj is Int -> 42 }`
     * where `$subj` is unresolved because there was no subject.
     */
    object UnresolvedWhenConditionSubject : KtFakeSourceElementKind()

    /**
     * for primary constructor parameter the corresponding class property is generated
     * with a fake sources which refers to this the corresponding parameter
     */
    object PropertyFromParameter : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * `if (true) 1` --> `if(true) { 1 }`
     * with a fake sources for the block which refers to the wrapped expression
     */
    object SingleExpressionBlock : KtFakeSourceElementKind()

    /**
     * this source is used for a single fake block created for indexed assignments expression,
     * see ImplicitUnit.IndexedAssignmentCoercion
     */
    object IndexedAssignmentCoercionBlock : KtFakeSourceElementKind()

    /**
     * Contract statements are wrapped in a special block to be reused between a contract FIR and a function body.
     */
    object ContractBlock : KtFakeSourceElementKind()

    /**
     * `x++` -> `x = x.inc()`
     * `x = x++` -> `x = { val <unary> = x; x = <unary>.inc(); <unary> }`
     */
    sealed class DesugaredIncrementOrDecrement : KtFakeSourceElementKind()
    object DesugaredPrefixInc : DesugaredIncrementOrDecrement()
    object DesugaredPrefixDec : DesugaredIncrementOrDecrement()
    object DesugaredPostfixInc : DesugaredIncrementOrDecrement()
    object DesugaredPostfixDec : DesugaredIncrementOrDecrement()

    /**
     * In `++a[1]`, `a.get(1)` will be called twice. This kind is used for the second call reference.
     */
    sealed class DesugaredPrefixSecondGetReference : KtFakeSourceElementKind()
    object DesugaredPrefixIncSecondGetReference : DesugaredPrefixSecondGetReference()
    object DesugaredPrefixDecSecondGetReference : DesugaredPrefixSecondGetReference()

    /**
     * `x !in list` --> `!(x in list)` where `!` and `!(x in list)` will have a fake source
     */
    object DesugaredInvertedContains : KtFakeSourceElementKind()

    /**
     * For data classes, FIR generates `componentN()` and `copy()` functions, which are covered by the various fake source element kinds
     * defined in this class.
     *
     * The fake element kinds must be kept in sync with [ALL_DATA_CLASS_GENERATED_MEMBERS].
     *
     * TODO (marco): Rename to `DataClassGeneratedMember`.
     */
    sealed class DataClassGeneratedMembers : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true) {
        /**
         * The source kind of a data class `componentN()` function. Its source is the corresponding constructor parameter for which the
         * component function has been generated.
         */
        object DataClassComponentFunction : DataClassGeneratedMembers()

        /**
         * The source kind of a data class `copy()` function. Its source is the corresponding data class.
         */
        object DataClassCopyFunction : DataClassGeneratedMembers()

        /**
         * The source kind of a parameter of the [DataClassCopyFunction]. Its source is the corresponding data class constructor parameter.
         */
        object DataClassCopyFunctionParameter : DataClassGeneratedMembers()

        /**
         * The source kind of a data class `equals(other: Any?)` function. Its source is the corresponding data class.
         */
        object DataClassEqualsFunction : DataClassGeneratedMembers()

        /**
         * The source kind of the parameter of the [DataClassEqualsFunction]. Its source is the corresponding data class.
         */
        object DataClassEqualsFunctionParameter : DataClassGeneratedMembers()

        /**
         * The source kind of a data class `hashCode()` function. Its source is the corresponding data class.
         */
        object DataClassHashCodeFunction : DataClassGeneratedMembers()

        /**
         * The source kind of a data class `toString()` function. Its source is the corresponding data class.
         */
        object DataClassToStringFunction : DataClassGeneratedMembers()
    }

    /**
     * For synthetic overrides implemented by delegation
     */
    object MembersImplementedByDelegation : KtFakeSourceElementKind()

    /**
     * `(vararg x: Int)` --> `(x: Array<out Int>)` where array type ref has a fake source kind
     */
    object ArrayTypeFromVarargParameter : KtFakeSourceElementKind()

    sealed class DestructuringInitializer : KtFakeSourceElementKind()

    /**
     * `val (a,b) = x` --> `val a = x.component1(); val b = x.component2()`
     * where componentN calls will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
     */
    object DesugaredComponentFunctionCall : DestructuringInitializer()

    /**
     * `(val a, val bb = b) = x` --> `val a = x.a; val bb = x.b`
     * where property accesses a and b will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
     */
    object DesugaredNameBasedDestructuring : DestructuringInitializer()

    /**
     * when smart casts applied to the expression, it is wrapped into FirSmartCastExpression
     * which type reference will have a fake source refer to a original source element of it
     */
    object SmartCastedTypeRef : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * when smart casts applied to the expression, it is wrapped into FirSmartCastExpression
     * this kind used for such FirSmartCastExpressions itself
     */
    object SmartCastExpression : KtFakeSourceElementKind()

    /**
     * for safe call expressions like a?.foo() the FirSafeCallExpression is generated
     * and it have a fake source
     */
    object DesugaredSafeCallExpression : KtFakeSourceElementKind()

    /**
     * `a > b` will be wrapped in FirComparisonExpression
     * with real source which points to initial `a > b` expression
     * and inner FirFunctionCall will refer to a fake source
     */
    object GeneratedComparisonExpression : KtFakeSourceElementKind()

    /**
     * `a ?: b` --> `when(val $subj = a) { .... }`
     * where `val $subj = a` has a fake source
     */
    object WhenGeneratedSubject : KtFakeSourceElementKind()

    /**
     * `list[0]` -> `list.get(0)` where name reference will have a fake source element
     */
    object ArrayAccessNameReference : KtFakeSourceElementKind()

    /**
     * `a += b` -> `a = a + b` or `a.plusAssign(b)`
     * `=`, `+`, and `plusAssign` will have a fake source element
     */
    sealed class DesugaredAugmentedAssign : KtFakeSourceElementKind()
    object DesugaredPlusAssign : DesugaredAugmentedAssign()
    object DesugaredMinusAssign : DesugaredAugmentedAssign()
    object DesugaredTimesAssign : DesugaredAugmentedAssign()
    object DesugaredDivAssign : DesugaredAugmentedAssign()
    object DesugaredRemAssign : DesugaredAugmentedAssign()

    object AssignmentPluginAltered : KtFakeSourceElementKind()

    /**
     * `a[b]++`
     * `b` -> `val <index0> = b` where `b` will have fake property
     */
    object ArrayIndexExpressionReference : KtFakeSourceElementKind()

    /**
     * `super.foo()` --> `super<Supertype>.foo()`
     * where `Supertype` has a fake source
     */
    object SuperCallImplicitType : KtFakeSourceElementKind()

    /**
     * `fun foo(vararg args: Int) {}`
     * `fun bar(1, 2, 3)` --> (resolved) `fun bar(VarargArgument(1, 2, 3))`
     */
    object VarargArgument : KtFakeSourceElementKind()

    /**
     * Part of desugared x?.y
     */
    object CheckedSafeCallSubject : KtFakeSourceElementKind()

    /**
     * `{ it + 1 }` --> `{ it -> it + 1 }`
     * where `it` parameter declaration has fake source
     */
    object ItLambdaParameter : KtFakeSourceElementKind()

    /**
     * For function type `context(Foo) () -> Unit`,
     * the context parameter with type `Foo` of the anonymous function.
     */
    object LambdaContextParameter : KtFakeSourceElementKind()

    /**
     * While it doesn't have an explicit source, it still has a type that might be a ConeErrorType
     */
    object LambdaReceiver : KtFakeSourceElementKind()

    /**
     * Example:
     *
     * ```kotlin
     * fun foo() {
     *     val (a, b) = listOf(1, 2)
     * }
     * ```
     *
     * When constructing the FIR for a destructuring declaration, we initially create an `FirBlock`
     * containing the properties `<destruct>`, `a`, and `b`.
     * If the original PSI is well-formed, this block is discarded,
     * and the properties are added to the outer block (i.e., to the function body).
     * However, if the PSI is invalid, this synthetic block may persist in the FIR tree.
     */
    object DestructuringBlock : KtFakeSourceElementKind()

    /**
     * `{ (a, b) -> foo() }` -> `{ x -> val (a, b) = x; { foo() } }`
     * where the inner block `{ foo() }` has fake source
     */
    object LambdaDestructuringBlock : KtFakeSourceElementKind()

    /**
     * for java annotations implicit constructor is generated
     * with a fake source which refers to containing class
     */
    object ImplicitJavaAnnotationConstructor : KtFakeSourceElementKind()

    /**
     * for FIR elements from Java enhancement
     */
    object Enhancement : KtFakeSourceElementKind()

    /**
     * for java annotations constructor implicit parameters are generated
     * with a fake source which refers to declared annotation methods
     */
    object ImplicitAnnotationAnnotationConstructorParameter : KtFakeSourceElementKind()

    /**
     * for java records implicit constructor is generated
     * with a fake source which refers to containing class
     */
    object ImplicitJavaRecordConstructor : KtFakeSourceElementKind()

    /**
     * for java record constructor implicit parameters are generated
     * with a fake source which refers to declared record components
     */
    object ImplicitRecordConstructorParameter : KtFakeSourceElementKind()

    /**
     * for java records implicit component functions are generated
     * with a fake source which refers to corresponding component
     */
    object JavaRecordComponentFunction : KtFakeSourceElementKind()

    /**
     * for java records implicit component fields are generated
     * with a fake source which refers to corresponding component
     */
    object JavaRecordComponentField : KtFakeSourceElementKind()

    /**
     * for the implicit field storing the delegated object for class delegation
     * with a fake source that refers to the KtExpression that creates the delegate
     */
    object ClassDelegationField : KtFakeSourceElementKind()

    /**
     * for annotation moved to another element due to annotation use-site target
     */
    object FromUseSiteTarget : KtFakeSourceElementKind()

    /**
     * for `@ParameterName` annotation call added to function types with names in the notation
     * with a fake source that refers to the value parameter in the function type notation
     * e.g., `(x: Int) -> Unit` becomes `Function1<@ParameterName("x") Int, Unit>`
     */
    object ParameterNameAnnotationCall : KtFakeSourceElementKind()

    /**
     * for implicit conversion from int to long with `.toLong` function
     * e.g. val x: Long = 1 + 1 becomes val x: Long = (1 + 1).toLong()
     */
    object IntToLongConversion : KtFakeSourceElementKind()

    /**
     * for extension receiver type the corresponding receiver parameter is generated
     * with a fake sources which refers to this the type
     */
    object ReceiverFromType : KtFakeSourceElementKind()

    /**
     * for all implicit receivers (now used for qualifiers only)
     */
    object ImplicitReceiver : KtFakeSourceElementKind()

    /**
     * for when on the LHS of an assignment an error expression appears
     */
    object AssignmentLValueError : KtFakeSourceElementKind()

    /**
     * For when the LHS of a desugared assignment has a null source.
     * In this case, the psi of [KtFakePsiSourceElement] should be set to the psi of the assignment
     */
    object DesugaredAssignmentLValueSourceIsNull : KtFakeSourceElementKind()

    /**
     * for return type of value parameters in lambdas
     */
    object ImplicitReturnTypeOfLambdaValueParameter : KtFakeSourceElementKind()

    /**
     * Synthetic calls for if/when/try/etc.
     */
    object SyntheticCall : KtFakeSourceElementKind()

    /**
     * When property doesn't have an initializer and explicit return type, but its getter's return type is specified
     */
    object PropertyTypeFromGetterReturnType : KtFakeSourceElementKind()

    /**
     * Scripts get implicit imports from their configurations
     */
    object ImplicitImport : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For provided parameters inside a script
     */
    object ScriptParameter : KtFakeSourceElementKind()

    /**
     * For script base class
     */
    object ScriptBaseClass : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For repl base class.
     */
    object ReplBaseClass : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For repl eval function.
     */
    object ReplEvalFunction : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * For repl result field.
     */
    object ReplResultField : KtFakeSourceElementKind(shouldSkipErrorTypeReporting = true)

    /**
     * When a lambda is converted to a SAM type, the expression is wrapped in an extra node
     */
    object SamConversion : KtFakeSourceElementKind()

    /**
     * For synthetic functions created for SAM constructors.
     */
    object SamConstructor : KtFakeSourceElementKind()

    /**
     * For it.functionFromAny() calls on a stub type
     */
    object CastToAnyForStubTypes : KtFakeSourceElementKind()

    /**
     * We use the whole context parameter as the fake source for default values.
     */
    object ContextParameterDefaultValue : KtFakeSourceElementKind()

    /**
     * For plugin-generated things
     */
    object PluginGenerated : KtFakeSourceElementKind()

    /**
     * To store diagnostic for erroneously resolved `arrayOf` which is being transformed to array literal.
     * Note that this may happen both with original `arrayOf` and with synthetic `arrayOf` itself created to resolve array literal.
     */
    object ErrorExpressionForTransformedArrayOf : KtFakeSourceElementKind()

    /**
     * To store some diagnostic for erroneously resolved top-level lambda
     * See [org.jetbrains.kotlin.config.LanguageFeature.ResolveTopLevelLambdasAsSyntheticCallArgument] and its usages
     */
    object ErrorExpressionForTopLevelLambda : KtFakeSourceElementKind()

    /**
     * To store diagnostics for erroneously resolved top-level collection literals.
     */
    object ErrorExpressionForTopLevelCollectionLiteral : KtFakeSourceElementKind()

    /**
     * Arbitrary error expression for which we failed to build the real PSI.
     */
    object ErrorExpression : KtFakeSourceElementKind()

    /**
     * When resolving ENTRY as `MyEnum.ENTRY` this is used for the `MyEnum` part
     */
    object QualifierForContextSensitiveResolution : KtFakeSourceElementKind()

    /**
     * When resolving a collection literal, a collection package qualifier or the explicit companion object added to the call as the explicit receiver.
     */
    object DesugaredReceiverForOperatorOfCall : KtFakeSourceElementKind()

    /**
     * When resolving a collection literal, this is used as a source for the generated callee reference.
     */
    object CalleeReferenceForOperatorOfCall : KtFakeSourceElementKind()

    // Moving these properties to the companion objects of their respective classes such as `DataClassGeneratedMembers` is not an option
    // because then the sealed class can be used as an object (e.g. as a `when` subject), which can lead to programming errors.
    companion object {
        val ALL_ENUM_GENERATED_DECLARATIONS: Set<EnumGeneratedDeclaration> = setOf(
            EnumGeneratedDeclaration.EnumValuesFunction,
            EnumGeneratedDeclaration.EnumValuesFunctionReturnType,
            EnumGeneratedDeclaration.EnumValueOfFunction,
            EnumGeneratedDeclaration.EnumValueOfFunctionParameter,
            EnumGeneratedDeclaration.EnumValueOfFunctionReturnType,
            EnumGeneratedDeclaration.EnumEntriesProperty,
            EnumGeneratedDeclaration.EnumEntriesPropertyReturnType,
            EnumGeneratedDeclaration.EnumEntriesPropertyGetter,
            EnumGeneratedDeclaration.EnumEntriesPropertyGetterReturnType,
            EnumGeneratedDeclaration.EnumCloneFunction,
        )

        val ALL_DATA_CLASS_GENERATED_MEMBERS: Set<DataClassGeneratedMembers> = setOf(
            DataClassGeneratedMembers.DataClassComponentFunction,
            DataClassGeneratedMembers.DataClassCopyFunction,
            DataClassGeneratedMembers.DataClassCopyFunctionParameter,
            DataClassGeneratedMembers.DataClassEqualsFunction,
            DataClassGeneratedMembers.DataClassEqualsFunctionParameter,
            DataClassGeneratedMembers.DataClassHashCodeFunction,
            DataClassGeneratedMembers.DataClassToStringFunction,
        )
    }
}

sealed class AbstractKtSourceElement {
    abstract val startOffset: Int
    abstract val endOffset: Int
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractKtSourceElement) return false

        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startOffset
        result = 31 * result + endOffset
        return result
    }
}

class KtOffsetsOnlySourceElement(
    override val startOffset: Int,
    override val endOffset: Int,
) : AbstractKtSourceElement()

// TODO: consider renaming to something like AstBasedSourceElement
sealed class KtSourceElement : AbstractKtSourceElement() {
    abstract val elementType: IElementType?
    abstract val kind: KtSourceElementKind
    abstract val lighterASTNode: LighterASTNode
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>

    abstract fun getElementTextInContextForDebug(): String

    /** Implementation must compute the hashcode from the source element. */
    abstract override fun hashCode(): Int

    /** Elements of the same source should be considered equal. */
    abstract override fun equals(other: Any?): Boolean
}

// NB: in certain situations, psi.node could be null (see e.g. KT-44152)
// Potentially exceptions can be provoked by elementType / lighterASTNode
sealed class KtPsiSourceElement(val psi: PsiElement) : KtSourceElement() {
    companion object {
        @JvmStatic
        private val lighterASTNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(
            KtPsiSourceElement::class.java,
            LighterASTNode::class.java,
            "_lighterASTNode"
        )

        @JvmStatic
        private val treeStructureNodeUpdater = AtomicReferenceFieldUpdater.newUpdater(
            KtPsiSourceElement::class.java,
            FlyweightCapableTreeStructure::class.java,
            "_treeStructure"
        )
    }

    override val elementType: IElementType?
        get() = psi.node?.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

    @Volatile
    private var _lighterASTNode: LighterASTNode? = null
    final override val lighterASTNode: LighterASTNode
        get() {
            _lighterASTNode?.let { return it }
            lighterASTNodeUpdater.compareAndSet(
                /* obj = */ this,
                /* expect = */ null,
                /* update = */ TreeBackedLighterAST.wrap(psi.node)
            )
            return _lighterASTNode!!
        }

    @Volatile
    private var _treeStructure: FlyweightCapableTreeStructure<LighterASTNode>? = null
    final override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>
        get() {
            _treeStructure?.let { return it }
            treeStructureNodeUpdater.compareAndSet(
                /* obj = */ this,
                /* expect = */ null,
                /* update = */ WrappedTreeStructure(psi.containingFile)
            )
            return _treeStructure!!
        }

    override fun getElementTextInContextForDebug(): String {
        return getElementTextWithContext(psi)
    }

    internal class WrappedTreeStructure(file: PsiFile) : FlyweightCapableTreeStructure<LighterASTNode> {
        private val lighterAST = TreeBackedLighterAST(file.node)

        fun unwrap(node: LighterASTNode) = lighterAST.unwrap(node)

        override fun toString(node: LighterASTNode): CharSequence = unwrap(node).text

        override fun getRoot(): LighterASTNode = lighterAST.root

        override fun getParent(node: LighterASTNode): LighterASTNode? =
            unwrap(node).psi.parent?.node?.let { TreeBackedLighterAST.wrap(it) }

        override fun getChildren(node: LighterASTNode, nodesRef: Ref<Array<LighterASTNode>>): Int {
            val psi = unwrap(node).psi
            val children = mutableListOf<PsiElement>()
            var child = psi.firstChild
            while (child != null) {
                children += child
                child = child.nextSibling
            }
            if (children.isEmpty()) {
                nodesRef.set(LighterASTNode.EMPTY_ARRAY)
            } else {
                nodesRef.set(children.map { TreeBackedLighterAST.wrap(it.node) }.toTypedArray())
            }
            return children.size
        }

        override fun disposeChildren(p0: Array<out LighterASTNode>?, p1: Int) {
        }

        override fun getStartOffset(node: LighterASTNode): Int {
            return getStartOffset(unwrap(node).psi)
        }

        private fun getStartOffset(element: PsiElement): Int {
            var child = element.firstChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.nextSibling
                }
                if (child != null) {
                    return getStartOffset(child)
                }
            }
            return element.textRange.startOffset
        }

        override fun getEndOffset(node: LighterASTNode): Int {
            return getEndOffset(unwrap(node).psi)
        }

        private fun getEndOffset(element: PsiElement): Int {
            var child = element.lastChild
            if (child != null) {
                while (child is PsiComment || child is PsiWhiteSpace) {
                    child = child.prevSibling
                }
                if (child != null) {
                    return getEndOffset(child)
                }
            }
            return element.textRange.endOffset
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtPsiSourceElement

        if (psi != other.psi) return false

        return true
    }

    override fun hashCode(): Int {
        return psi.hashCode()
    }
}

class KtRealPsiSourceElement(psi: PsiElement) : KtPsiSourceElement(psi) {
    override val kind: KtSourceElementKind get() = KtRealSourceElementKind
}

/**
 * Checking for [KtFakePsiSourceElement] only works for PSI sources.
 *
 * To check for a fake source regardless of source type, check if [KtSourceElement.kind] is a [KtFakeSourceElementKind].
 */
@RequiresOptIn
annotation class SuspiciousFakeSourceCheck

@SuspiciousFakeSourceCheck
open class KtFakePsiSourceElement(
    psi: PsiElement,
    override val kind: KtFakeSourceElementKind,
) : KtPsiSourceElement(psi) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as KtFakePsiSourceElement

        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

@SuspiciousFakeSourceCheck
class KtFakePsiSourceElementWithCustomOffsetStrategy(
    psi: PsiElement,
    kind: KtFakeSourceElementKind,
    val strategy: KtSourceElementOffsetStrategy.Custom,
) : KtFakePsiSourceElement(psi, kind) {
    override val startOffset: Int
        get() = strategy.startOffset

    override val endOffset: Int
        get() = strategy.endOffset

    override fun equals(other: Any?): Boolean = this === other ||
            other is KtFakePsiSourceElementWithCustomOffsetStrategy &&
            super.equals(other) &&
            strategy == other.strategy

    override fun hashCode(): Int = 31 * super.hashCode() + strategy.hashCode()
}

/**
 * Represents a strategy for getting offsets for [KtSourceElement]s.
 *
 * @see fakeElement
 */
sealed class KtSourceElementOffsetStrategy {
    /**
     * The default strategy, which uses offsets from the original element.
     */
    data object Default : KtSourceElementOffsetStrategy()

    /**
     * Represents a strategy with custom [startOffset] and [endOffset]s.
     */
    sealed class Custom : KtSourceElementOffsetStrategy() {
        abstract val startOffset: Int
        abstract val endOffset: Int

        class Initialized(override val startOffset: Int, override val endOffset: Int) : Custom() {
            override fun equals(other: Any?): Boolean = this === other ||
                    other is Initialized &&
                    startOffset == other.startOffset &&
                    endOffset == other.endOffset

            override fun hashCode(): Int = 31 * startOffset.hashCode() + endOffset.hashCode()
        }

        class Delegated(val startOffsetAnchor: KtSourceElement, val endOffsetAnchor: KtSourceElement) : Custom() {
            override val startOffset: Int
                get() = startOffsetAnchor.startOffset

            override val endOffset: Int
                get() = endOffsetAnchor.endOffset

            override fun equals(other: Any?): Boolean = this === other ||
                    other is Delegated &&
                    startOffsetAnchor == other.startOffsetAnchor &&
                    endOffsetAnchor == other.endOffsetAnchor

            override fun hashCode(): Int = 31 * startOffsetAnchor.hashCode() + endOffsetAnchor.hashCode()
        }
    }
}

fun KtSourceElement.fakeElement(
    newKind: KtFakeSourceElementKind,
    offsetStrategy: KtSourceElementOffsetStrategy = KtSourceElementOffsetStrategy.Default,
): KtSourceElement {
    if (kind == newKind) return this
    return when (this) {
        is KtLightSourceElement -> {
            val (startOffset, endOffset) = if (offsetStrategy is KtSourceElementOffsetStrategy.Custom) {
                offsetStrategy.startOffset to offsetStrategy.endOffset
            } else {
                startOffset to endOffset
            }

            KtLightSourceElement(
                lighterASTNode,
                startOffset,
                endOffset,
                treeStructure,
                newKind
            )
        }

        is KtPsiSourceElement -> when (offsetStrategy) {
            is KtSourceElementOffsetStrategy.Default -> KtFakePsiSourceElement(psi, newKind)
            is KtSourceElementOffsetStrategy.Custom -> KtFakePsiSourceElementWithCustomOffsetStrategy(psi, newKind, offsetStrategy)
        }
    }
}

fun KtSourceElement.realElement(): KtSourceElement = when (this) {
    is KtRealPsiSourceElement -> this
    is KtLightSourceElement -> KtLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, KtRealSourceElementKind)
    is KtPsiSourceElement -> KtRealPsiSourceElement(psi)
}


class KtLightSourceElement(
    override val lighterASTNode: LighterASTNode,
    override val startOffset: Int,
    override val endOffset: Int,
    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
    override val kind: KtSourceElementKind = KtRealSourceElementKind,
) : KtSourceElement() {
    override val elementType: IElementType
        get() = lighterASTNode.tokenType

    /**
     * We can create a [KtLightSourceElement] from a [KtPsiSourceElement] by using [KtPsiSourceElement.lighterASTNode];
     * [unwrapToKtPsiSourceElement] allows to get original [KtPsiSourceElement] in such case.
     *
     * If it is `pure` [KtLightSourceElement], i.e, compiler created it in light tree mode, then return [unwrapToKtPsiSourceElement] `null`.
     * Otherwise, return some not-null result.
     */
    fun unwrapToKtPsiSourceElement(): KtPsiSourceElement? {
        if (treeStructure !is KtPsiSourceElement.WrappedTreeStructure) return null
        val node = treeStructure.unwrap(lighterASTNode)
        return node.psi?.toKtPsiSourceElement(kind)
    }

    override fun getElementTextInContextForDebug(): String {
        return treeStructure.toString(lighterASTNode).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtLightSourceElement

        if (lighterASTNode != other.lighterASTNode) return false
        if (startOffset != other.startOffset) return false
        if (endOffset != other.endOffset) return false
        if (treeStructure != other.treeStructure) return false
        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lighterASTNode.hashCode()
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        result = 31 * result + treeStructure.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

val AbstractKtSourceElement?.psi: PsiElement? get() = (this as? KtPsiSourceElement)?.psi

val KtSourceElement?.text: CharSequence?
    get() = when (this) {
        is KtPsiSourceElement -> psi.text
        is KtLightSourceElement -> treeStructure.toString(lighterASTNode)
        else -> null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toKtPsiSourceElement(kind: KtSourceElementKind = KtRealSourceElementKind): KtPsiSourceElement = when (kind) {
    is KtRealSourceElementKind -> KtRealPsiSourceElement(this)
    is KtFakeSourceElementKind -> KtFakePsiSourceElement(this, kind)
}

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toKtLightSourceElement(
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    kind: KtSourceElementKind = KtRealSourceElementKind,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
): KtLightSourceElement = KtLightSourceElement(this, startOffset, endOffset, tree, kind)

fun sourceKindForIncOrDec(operation: Name, isPrefix: Boolean) = when (operation) {
    OperatorNameConventions.INC -> if (isPrefix) {
        KtFakeSourceElementKind.DesugaredPrefixInc
    } else {
        KtFakeSourceElementKind.DesugaredPostfixInc
    }
    OperatorNameConventions.DEC -> if (isPrefix) {
        KtFakeSourceElementKind.DesugaredPrefixDec
    } else {
        KtFakeSourceElementKind.DesugaredPostfixDec
    }
    else -> error("Unexpected operator: ${operation.identifier}")
}
