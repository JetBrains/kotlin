// ISSUE: KT-44814
// WITH_STDLIB
// DUMP_IR
// DUMP_CFG: LEVELS

class FlyweightCapableTreeStructure

sealed class FirSourceElement {
    abstract val lighterASTNode: LighterASTNode
    abstract val treeStructure: FlyweightCapableTreeStructure
}
class FirPsiSourceElement(
    val psi: PsiElement,
    override val lighterASTNode: LighterASTNode,
    override val treeStructure: FlyweightCapableTreeStructure
) : FirSourceElement()
class FirLightSourceElement(
    override val lighterASTNode: LighterASTNode,
    override val treeStructure: FlyweightCapableTreeStructure
) : FirSourceElement()

open class PsiElement
class ASTNode
class LighterASTNode(val _children: List<LighterASTNode?> = emptyList()) {
    fun getChildren(treeStructure: FlyweightCapableTreeStructure): List<LighterASTNode?> = _children

    val tokenType: TokenType = TokenType.MODIFIER_LIST
}

class TokenType {
    companion object {
        val MODIFIER_LIST = TokenType()
    }
}

class KtModifierKeywordToken
class KtModifierList : PsiElement()
class KtModifierListOwner : PsiElement() {
    val modifierList: KtModifierList = KtModifierList()
}

internal sealed class FirModifier<Node : Any>(val node: Node, val token: KtModifierKeywordToken) {
    class FirPsiModifier(
        node: ASTNode,
        token: KtModifierKeywordToken
    ) : FirModifier<ASTNode>(node, token)

    class FirLightModifier(
        node: LighterASTNode,
        token: KtModifierKeywordToken,
        val tree: FlyweightCapableTreeStructure
    ) : FirModifier<LighterASTNode>(node, token)
}

internal sealed class FirModifierList {
    val modifiers: List<FirModifier<*>> = emptyList()

    class FirPsiModifierList(val modifierList: KtModifierList) : FirModifierList()

    class FirLightModifierList(val modifierList: LighterASTNode, val tree: FlyweightCapableTreeStructure) : FirModifierList()

    companion object {
        fun FirSourceElement?.getModifierList(): FirModifierList? {
            return when (this) {
                null -> null
                is FirPsiSourceElement-> (psi as? KtModifierListOwner)?.modifierList?.let { FirPsiModifierList(it) }
                is FirLightSourceElement -> {
                    val modifierListNode = lighterASTNode.getChildren(treeStructure).find { it?.tokenType == TokenType.MODIFIER_LIST }
                        ?: return null // error is here
                    FirLightModifierList(modifierListNode, treeStructure)
                }
            }
        }

        fun boxImpl(): String {
            val sourceElement: FirSourceElement? = FirLightSourceElement(LighterASTNode(listOf(LighterASTNode())), FlyweightCapableTreeStructure())
            val result = sourceElement.getModifierList()
            return if (result is FirLightModifierList) "OK" else "Fail"
        }
    }
}

fun box(): String {
    return FirModifierList.boxImpl()
}
