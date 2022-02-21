/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

sealed class KtSourceElementKind

object KtRealSourceElementKind : KtSourceElementKind()

sealed class KtFakeSourceElementKind : KtSourceElementKind() {
    // for some fir expression implicit return typeRef is generated
    // some of them are: break, continue, return, throw, string concat,
    // destruction parameters, function literals, explicitly boolean expressions
    object ImplicitTypeRef : KtFakeSourceElementKind()

    // for each class special class self type ref is created
    // and have a fake source referencing it
    object ClassSelfTypeRef : KtFakeSourceElementKind()

    // FirErrorTypeRef may be built using unresolved firExpression
    // and have a fake source referencing it
    object ErrorTypeRef : KtFakeSourceElementKind()

    // for properties without accessors default getter & setter are generated
    // they have a fake source which refers to property
    object DefaultAccessor : KtFakeSourceElementKind()

    // for delegated properties, getter & setter calls to the delegate
    // they have a fake source which refers to the call that creates the delegate
    object DelegatedPropertyAccessor : KtFakeSourceElementKind()

    // for kt classes without implicit primary constructor one is generated
    // with a fake source which refers to containing class
    object ImplicitConstructor : KtFakeSourceElementKind()

    // for constructors which do not have delegated constructor call the fake one is generated
    // with a fake sources which refers to the original constructor
    object DelegatingConstructorCall : KtFakeSourceElementKind()

    // for enum entry with bodies the initializer in a form of anonymous object is generated
    // with a fake sources which refers to the enum entry
    object EnumInitializer : KtFakeSourceElementKind()

    // for lambdas with implicit return the return statement is generated which is labeled
    // with a fake sources which refers to the target expression
    object GeneratedLambdaLabel : KtFakeSourceElementKind()

    // for lambdas & functions with expression bodies the return statement is added
    // with a fake sources which refers to the return target
    object ImplicitReturn : KtFakeSourceElementKind()

    // return expression in procedures -> return Unit
    // with a fake sources which refers to the return statement
    object ImplicitUnit : KtFakeSourceElementKind()

    // delegates are wrapped into FirWrappedDelegateExpression
    // with a fake sources which refers to delegated expression
    object WrappedDelegate : KtFakeSourceElementKind()

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
    object DesugaredForLoop : KtFakeSourceElementKind()

    object ImplicitInvokeCall : KtFakeSourceElementKind()

    // Consider an atomic qualified access like `i`. In the FIR tree, both the FirQualifiedAccessExpression and its calleeReference uses
    // `i` as the source. Hence, this fake kind is set on the `calleeReference` to make sure no PSI element is shared by multiple FIR
    // elements. This also applies to `this` and `super` references.
    object ReferenceInAtomicQualifiedAccess : KtFakeSourceElementKind()

    // for enum classes we have valueOf & values functions generated
    // with a fake sources which refers to this the enum class
    object EnumGeneratedDeclaration : KtFakeSourceElementKind()

    // when (x) { "abc" -> 42 } --> when(val $subj = x) { $subj == "abc" -> 42 }
    // where $subj == "42" has fake psi source which refers to "42" as inner expression
    // and $subj fake source refers to "42" as KtWhenCondition
    object WhenCondition : KtFakeSourceElementKind()


    // for primary constructor parameter the corresponding class property is generated
    // with a fake sources which refers to this the corresponding parameter
    object PropertyFromParameter : KtFakeSourceElementKind()

    // if (true) 1 --> if(true) { 1 }
    // with a fake sources for the block which refers to the wrapped expression
    object SingleExpressionBlock : KtFakeSourceElementKind()

    // x++ -> x = x.inc()
    // x = x++ -> x = { val <unary> = x; x = <unary>.inc(); <unary> }
    object DesugaredIncrementOrDecrement : KtFakeSourceElementKind()

    // x !in list --> !(x in list) where ! and !(x in list) will have a fake source
    object DesugaredInvertedContains : KtFakeSourceElementKind()

    // for data classes fir generates componentN() & copy() functions
    // for componentN() functions the source will refer to the corresponding param and will be marked as a fake one
    // for copy() functions the source will refer class to the param and will be marked as a fake one
    object DataClassGeneratedMembers : KtFakeSourceElementKind()

    // (vararg x: Int) --> (x: Array<out Int>) where array type ref has a fake source kind
    object ArrayTypeFromVarargParameter : KtFakeSourceElementKind()

    // val (a,b) = x --> val a = x.component1(); val b = x.component2()
    // where componentN calls will have the fake source elements refer to the corresponding KtDestructuringDeclarationEntry
    object DesugaredComponentFunctionCall : KtFakeSourceElementKind()

    // when smart casts applied to the expression, its wrapped into FirExpressionWithSmartcast
    // which type reference will have a fake source refer to a original source element of it
    object SmartCastedTypeRef : KtFakeSourceElementKind()

    // for safe call expressions like a?.foo() the FirSafeCallExpression is generated
    // and it have a fake source
    object DesugaredSafeCallExpression : KtFakeSourceElementKind()

    // a += 2 --> a = a + 2
    // where a + 2 will have a fake source
    object DesugaredCompoundAssignment : KtFakeSourceElementKind()

    // `a > b` will be wrapped in FirComparisonExpression
    // with real source which points to initial `a > b` expression
    // and inner FirFunctionCall will refer to a fake source
    object GeneratedComparisonExpression : KtFakeSourceElementKind()

    // a ?: b --> when(val $subj = a) { .... }
    // where `val $subj = a` has a fake source
    object WhenGeneratedSubject : KtFakeSourceElementKind()

    // list[0] -> list.get(0) where name reference will have a fake source element
    object ArrayAccessNameReference : KtFakeSourceElementKind()

    // super.foo() --> super<Supertype>.foo()
    // where `Supertype` has a fake source
    object SuperCallImplicitType : KtFakeSourceElementKind()

    // Consider `super<Supertype>.foo()`. The source PSI `Supertype` is referenced by both the qualified access expression
    // `super<Supertype>` and the calleeExpression `super<Supertype>`. To avoid having two FIR elements sharing the same source, this fake
    // source is assigned to the qualified access expression.
    object SuperCallExplicitType : KtFakeSourceElementKind()

    // fun foo(vararg args: Int) {}
    // fun bar(1, 2, 3) --> [resolved] fun bar(VarargArgument(1, 2, 3))
    object VarargArgument : KtFakeSourceElementKind()

    // Part of desugared x?.y
    object CheckedSafeCallSubject : KtFakeSourceElementKind()

    // { it + 1} --> { it -> it + 1 }
    // where `it` parameter declaration has fake source
    object ItLambdaParameter : KtFakeSourceElementKind()

    // for java annotations implicit constructor is generated
    // with a fake source which refers to containing class
    object ImplicitJavaAnnotationConstructor : KtFakeSourceElementKind()

    // for java annotations constructor implicit parameters are generated
    // with a fake source which refers to declared annotation methods
    object ImplicitAnnotationAnnotationConstructorParameter : KtFakeSourceElementKind()

    // for the implicit field storing the delegated object for class delegation
    // with a fake source that refers to the KtExpression that creates the delegate
    object ClassDelegationField : KtFakeSourceElementKind()

    // for annotation moved to another element due to annotation use-site target
    object FromUseSiteTarget : KtFakeSourceElementKind()

    // for `@ParameterName` annotation call added to function types with names in the notation
    // with a fake source that refers to the value parameter in the function type notation
    // e.g., `(x: Int) -> Unit` becomes `Function1<@ParameterName("x") Int, Unit>`
    object ParameterNameAnnotationCall : KtFakeSourceElementKind()

    // for implicit conversion from int to long with `.toLong` function
    // e.g. val x: Long = 1 + 1 becomes val x: Long = (1 + 1).toLong()
    object IntToLongConversion : KtFakeSourceElementKind()
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
    abstract val elementType: IElementType
    abstract val kind: KtSourceElementKind
    abstract val lighterASTNode: LighterASTNode
    abstract val treeStructure: FlyweightCapableTreeStructure<LighterASTNode>

    /** Implementation must compute the hashcode from the source element. */
    abstract override fun hashCode(): Int

    /** Elements of the same source should be considered equal. */
    abstract override fun equals(other: Any?): Boolean
}

// NB: in certain situations, psi.node could be null
// Potentially exceptions can be provoked by elementType / lighterASTNode
sealed class KtPsiSourceElement(val psi: PsiElement) : KtSourceElement() {
    override val elementType: IElementType
        get() = psi.node.elementType

    override val startOffset: Int
        get() = psi.textRange.startOffset

    override val endOffset: Int
        get() = psi.textRange.endOffset

    override val lighterASTNode by lazy { TreeBackedLighterAST.wrap(psi.node) }

    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> by lazy { WrappedTreeStructure(psi.containingFile) }

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

class KtFakeSourceElement(psi: PsiElement, override val kind: KtFakeSourceElementKind) : KtPsiSourceElement(psi) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as KtFakeSourceElement

        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }
}

fun KtSourceElement.fakeElement(newKind: KtFakeSourceElementKind): KtSourceElement {
    if (kind == newKind) return this
    return when (this) {
        is KtLightSourceElement -> KtLightSourceElement(lighterASTNode, startOffset, endOffset, treeStructure, newKind)
        is KtPsiSourceElement -> KtFakeSourceElement(psi, newKind)
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
    is KtFakeSourceElementKind -> KtFakeSourceElement(this, kind)
}

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toKtLightSourceElement(
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    kind: KtSourceElementKind = KtRealSourceElementKind,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset
): KtLightSourceElement = KtLightSourceElement(this, startOffset, endOffset, tree, kind)


class KtSourceFilePosition(val line: Int, val column: Int, val lineContent: String?) {

    // NOTE: This method is used for presenting positions to the user
    override fun toString(): String = if (line < 0) "(offset: $column line unknown)" else "($line,$column)"

    companion object {
        val NONE = KtSourceFilePosition(-1, -1, null)
    }
}

class SequentialFilePositionFinder(file: File) : Closeable {

    private var reader: InputStreamReader = file.reader(/* TODO: select proper charset */)

    private var currentLineContent: String? = null
    private val buffer = CharArray(255)
    private var bufLength = -1
    private var bufPos = 0
    private var endOfStream = false
    private var skipNextLf = false

    private var charsRead = 0
    private var currentLine = 0

    // assuming that if called multiple times, calls should be sorted by ascending offset
    fun findNextPosition(offset: Int, withLineContents: Boolean = true): KtSourceFilePosition {
        assert(offset >= charsRead - (currentLineContent?.length ?: 0))

        fun posInCurrentLine(): KtSourceFilePosition? {
            val col = offset - (charsRead - currentLineContent!!.length - 1)/* beginning of line offset */ + 1 /* col is 1-based */
            return if (col <= currentLineContent!!.length)
                KtSourceFilePosition(currentLine, col, if (withLineContents) currentLineContent else null)
            else null
        }

        if (offset < charsRead) {
            return posInCurrentLine()!!
        }

        while (true) {
            if (currentLineContent == null) {
                currentLineContent = readNextLine()
            }

            posInCurrentLine()?.let { return@findNextPosition it }

            if (endOfStream) return KtSourceFilePosition(-1, offset, if (withLineContents) currentLineContent else null)

            currentLineContent = null
        }
    }

    private fun readNextLine() = buildString {
        while (true) {
            if (bufPos >= bufLength) {
                bufLength = reader.read(buffer)
                bufPos = 0
                if (bufLength < 0) {
                    endOfStream = true
                    break
                }
            } else {
                val c = buffer[bufPos++]
                charsRead++
                when {
                    c == '\n' && skipNextLf -> {
                        skipNextLf = false
                    }
                    c == '\n' || c == '\r' -> {
                        currentLine++
                        skipNextLf = c == '\r'
                        break
                    }
                    else -> {
                        append(c)
                        skipNextLf = false
                    }
                }
            }
        }
    }

    override fun close() {
        reader.close()
    }
}
