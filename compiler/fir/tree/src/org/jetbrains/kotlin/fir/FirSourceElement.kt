/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

sealed class FirSourceElementKind

object FirRealSourceElementKind : FirSourceElementKind()

sealed class FirFakeSourceElementKind : FirSourceElementKind() {
    // for some fir expression implicit return typeRef is generated
    // some of them are: break, continue, return, throw, string concat,
    // destruction parameters, function literals, explicitly boolean expressions
    object ImplicitTypeRef : FirFakeSourceElementKind()

    // for each class special class self type ref is created
    // and have a fake source referencing it
    object ClassSelfTypeRef : FirFakeSourceElementKind()

    // FirErrorTypeRef may be built using unresolved firExpression
    // and have a fake source referencing it
    object ErrorTypeRef : FirFakeSourceElementKind()

    // for properties without accessors default getter & setter are generated
    // they have a fake source which refers to property
    object DefaultAccessor : FirFakeSourceElementKind()

    // for kt classes without implicit primary constructor one is generated
    // with a fake source which refers to containing class
    object ImplicitConstructor : FirFakeSourceElementKind()

    // for constructors which do not have delegated constructor call the fake one is generated
    // with a fake sources which refers to the original constructor
    object DelegatingConstructorCall : FirFakeSourceElementKind()

    // for enum entry with bodies the initializer in a form of anonymous object is generated
    // with a fake sources which refers to the enum entry
    object EnumInitializer : FirFakeSourceElementKind()

    // for lambdas with implicit return the return statement is generated which is labeled
    // with a fake sources which refers to the target expression
    object GeneratedLambdaLabel : FirFakeSourceElementKind()

    // for lambdas & functions with expression bodies the return statement is added
    // with a fake sources which refers to the return target
    object ImplicitReturn : FirFakeSourceElementKind()

    // return expression in procedures -> return Unit
    // with a fake sources which refers to the return statement
    object ImplicitUnit : FirFakeSourceElementKind()

    // delegates are wrapped into FirWrappedDelegateExpression
    // with a fake sources which refers to delegated expression
    object WrappedDelegate : FirFakeSourceElementKind()

    //  `for (i in list) { println(i) }` is converted to
    //  ```
    //  val <iterator>: = list.iterator()
    //  while(<iterator>.hasNext()) {
    //    val i = <iterator>.next()
    //    println(i)
    //  }
    //  ```
    //  where the generated WHILE loop has source element of initial FOR loop,
    //  other generated elements are marked as fake ones
    object DesugaredForLoop : FirFakeSourceElementKind()

    object ImplicitInvokeCall : FirFakeSourceElementKind()

    // this/super expressions have FirThisReference/FirSuperReference
    // with a fake sources which refers to this this/super expression
    object ExplicitThisOrSuperReference : FirFakeSourceElementKind()

    // for enum classes we have valueOf & values functions generated
    // with a fake sources which refers to this the enum class
    object EnumGeneratedDeclaration : FirFakeSourceElementKind()

    // when (x) { "abc" -> 42 } --> when(val $subj = x) { $subj == "abc" -> 42 }
    // where $subj == "42" has fake psi source which refers to "42" as inner expression
    // and $subj fake source refers to "42" as KtWhenCondition
    object WhenCondition : FirFakeSourceElementKind()


    // for primary constructor parameter the corresponding class property is generated
    // with a fake sources which refers to this the corresponding parameter
    object PropertyFromParameter : FirFakeSourceElementKind()

    // if (true) 1 --> if(true) { 1 }
    // with a fake sources for the block which refers to the wrapped expression
    object SingleExpressionBlock : FirFakeSourceElementKind()

    // x++ -> x = x.inc()
    // x = x++ -> x = { val <unary> = x; x = <unary>.inc(); <unary> }
    object DesugaredIncrementOrDecrement : FirFakeSourceElementKind()

    // x !in list --> !(x in list) where ! and !(x in list) will have a fake source
    object DesugaredInvertedContains : FirFakeSourceElementKind()

    // for data classes fir generates componentN() & copy() functions
    // for componentN() functions the source will refer to the corresponding param and will be marked as a fake one
    // for copy() functions the source will refer class to the param and will be marked as a fake one
    object DataClassGeneratedMembers : FirFakeSourceElementKind()

    // (vararg x: Int) --> (x: Array<out Int>) where array type ref has a fake source kind
    object ArrayTypeFromVarargParameter : FirFakeSourceElementKind()

    // val (a,b) = x --> val a = x.component1(); val b = x.component2()
    // where componentN calls will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
    object DesugaredComponentFunctionCall : FirFakeSourceElementKind()

    // when smart casts applied to the expression, its wrapped into FirExpressionWithSmartcast
    // which type reference will have a fake source refer to a original source element of it
    object SmartCastedTypeRef : FirFakeSourceElementKind()

    // for safe call expressions like a?.foo() the FirSafeCallExpression is generated
    // and it have a fake source
    object DesugaredSafeCallExpression : FirFakeSourceElementKind()

    // a += 2 --> a = a + 2
    // where a + 2 will have a fake source
    object DesugaredCompoundAssignment : FirFakeSourceElementKind()

    //"$a" --> a.toString() where toString call source is marked as a fake one
    object GeneratedToStringCallOnTemplateEntry : FirFakeSourceElementKind()

    // `a > b` will be wrapped in FirComparisonExpression
    // with real source which points to initial `a > b` expression
    // and inner FirFunctionCall will refer to a fake source
    object GeneratedComparisonExpression : FirFakeSourceElementKind()

    // a ?: b --> when(val $subj = a) { .... }
    // where `val $subj = a` has a fake source
    object WhenGeneratedSubject : FirFakeSourceElementKind()

    // list[0] -> list.get(0) where name reference will have a fake source element
    object ArrayAccessNameReference : FirFakeSourceElementKind()

    // super.foo() --> super<Supertype>.foo()
    // where `Supertype` has a fake source
    object SuperCallImplicitType : FirFakeSourceElementKind()
}

sealed class FirSourceElement {
    abstract val elementType: IElementType
    abstract val startOffset: Int
    abstract val endOffset: Int
    abstract val kind: FirSourceElementKind
}


sealed class FirPsiSourceElement<out P : PsiElement>(val psi: P) : FirSourceElement() {
    override val elementType: IElementType
        get() = psi.node.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset
}

class FirRealPsiSourceElement<out P : PsiElement>(psi: P) : FirPsiSourceElement<P>(psi) {
    override val kind: FirSourceElementKind get() = FirRealSourceElementKind
}

class FirFakeSourceElement<out P : PsiElement>(psi: P, override val kind: FirFakeSourceElementKind) : FirPsiSourceElement<P>(psi)

fun FirSourceElement.fakeElement(newKind: FirFakeSourceElementKind): FirSourceElement {
    return when (this) {
        is FirLightSourceElement -> FirLightSourceElement(element, startOffset, endOffset, tree, newKind)
        is FirPsiSourceElement<*> -> FirFakeSourceElement(psi, newKind)
    }
}

class FirLightSourceElement(
    val element: LighterASTNode,
    override val startOffset: Int,
    override val endOffset: Int,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    override val kind: FirSourceElementKind = FirRealSourceElementKind,
) : FirSourceElement() {
    override val elementType: IElementType
        get() = element.tokenType
}

val FirSourceElement?.psi: PsiElement? get() = (this as? FirPsiSourceElement<*>)?.psi

val FirElement.psi: PsiElement? get() = (source as? FirPsiSourceElement<*>)?.psi
val FirElement.realPsi: PsiElement? get() = (source as? FirRealPsiSourceElement<*>)?.psi

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toFirPsiSourceElement(kind: FirSourceElementKind = FirRealSourceElementKind): FirPsiSourceElement<*> = when (kind) {
    is FirRealSourceElementKind -> FirRealPsiSourceElement(this)
    is FirFakeSourceElementKind -> FirFakeSourceElement(this, kind)
}

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toFirLightSourceElement(
    startOffset: Int, endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    kind: FirSourceElementKind = FirRealSourceElementKind
): FirLightSourceElement = FirLightSourceElement(this, startOffset, endOffset, tree, kind)

val FirSourceElement?.lightNode: LighterASTNode? get() = (this as? FirLightSourceElement)?.element
