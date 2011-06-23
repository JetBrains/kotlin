package org.jetbrains.jet.plugin.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class JetBlock implements ASTBlock {
    private ASTNode myNode;
    private Alignment myAlignment;
    private Indent myIndent;
    private Wrap myWrap;
    private CodeStyleSettings mySettings;
    private List<Block> mySubBlocks;

    private static final TokenSet CODE_BLOCKS = TokenSet.create(
            JetNodeTypes.BLOCK,
            JetNodeTypes.CLASS_BODY,
            JetNodeTypes.FUNCTION_LITERAL_EXPRESSION);

    private static final TokenSet STATEMENT_PARTS = TokenSet.create(
            JetNodeTypes.THEN,
            JetNodeTypes.ELSE);

    public JetBlock(ASTNode node, Alignment alignment, Indent indent, Wrap wrap, CodeStyleSettings settings) {
        myNode = node;
        myAlignment = alignment;
        myIndent = indent;
        myWrap = wrap;
        mySettings = settings;
    }

    @Override
    public ASTNode getNode() {
        return myNode;
    }

    @NotNull
    @Override
    public TextRange getTextRange() {
        return myNode.getTextRange();
    }

    @Override
    public Wrap getWrap() {
        return myWrap;
    }

    @Override
    public Indent getIndent() {
        return myIndent;
    }

    @Override
    public Alignment getAlignment() {
        return myAlignment;
    }

    @NotNull
    @Override
    public List<Block> getSubBlocks() {
        if (mySubBlocks == null) {
            mySubBlocks = buildSubBlocks();
        }
        return new ArrayList<Block>(mySubBlocks);
    }

    private List<Block> buildSubBlocks() {
        List<Block> blocks = new ArrayList<Block>();
        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {

          IElementType childType = child.getElementType();

          if (child.getTextRange().getLength() == 0) continue;

          if (childType == TokenType.WHITE_SPACE) {
            continue;
          }

          blocks.add(buildSubBlock(child));
        }
        return Collections.unmodifiableList(blocks);
    }

    private Block buildSubBlock(ASTNode child) {
        Wrap wrap = null;
        Indent childIndent = Indent.getNoneIndent();
        Alignment childAlignment = null;

        if (CODE_BLOCKS.contains(myNode.getElementType())) {
            childIndent = indentIfNotBrace(child);
        }
        else if (myNode.getElementType() == JetNodeTypes.WHEN) {
            childIndent = indentIfNotBrace(child);
        }
        else if (STATEMENT_PARTS.contains(myNode.getElementType()) && child.getElementType() != JetNodeTypes.BLOCK) {
            childIndent = Indent.getNormalIndent();
        }

        return new JetBlock(child, childAlignment, childIndent, wrap, mySettings);
    }

    private static Indent indentIfNotBrace(ASTNode child) {
        return child.getElementType() == JetTokens.RBRACE || child.getElementType() == JetTokens.LBRACE
                ? Indent.getNoneIndent()
                : Indent.getNormalIndent();
    }

    @Override
    public Spacing getSpacing(Block block, Block block1) {
        return null;
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        Indent childIndent = Indent.getNoneIndent();
        if (CODE_BLOCKS.contains(myNode.getElementType()) || myNode.getElementType() == JetNodeTypes.WHEN) {
            childIndent = Indent.getNormalIndent();
        }
        return new ChildAttributes(childIndent, null);
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }
}
