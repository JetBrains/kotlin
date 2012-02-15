/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @see Block for good JavaDoc documentation
 * @author yole
 */
public class JetBlock extends AbstractBlock {
    private Indent myIndent;
    private CodeStyleSettings mySettings;
    private final SpacingBuilder mySpacingBuilder;
    
    private List<Block> mySubBlocks;

    private static final TokenSet CODE_BLOCKS = TokenSet.create(
            JetNodeTypes.BLOCK,
            JetNodeTypes.CLASS_BODY,
            JetNodeTypes.FUNCTION_LITERAL_EXPRESSION);

    private static final TokenSet STATEMENT_PARTS = TokenSet.create(
            JetNodeTypes.THEN,
            JetNodeTypes.ELSE);
    
    // private static final List<IndentWhitespaceRule>

    public JetBlock(@NotNull ASTNode node,
            Alignment alignment,
            Indent indent,
            Wrap wrap,
            CodeStyleSettings settings,
            SpacingBuilder spacingBuilder) {

        super(node, wrap, alignment);
        myIndent = indent;
        mySettings = settings;
        mySpacingBuilder = spacingBuilder;
    }

    @Override
    public Indent getIndent() {
        return myIndent;
    }

    @Override
    protected List<Block> buildChildren() {
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

    @NotNull
    private Block buildSubBlock(@NotNull ASTNode child) {
        Wrap wrap = null;
        Indent childIndent = Indent.getNoneIndent();
        Alignment childAlignment = null;

        ASTNode childParent = child.getTreeParent();

        if (CODE_BLOCKS.contains(myNode.getElementType())) {
            childIndent = indentIfNotBrace(child);
        }
        else if (childParent != null &&
                 childParent.getElementType() == JetNodeTypes.BODY &&
                 child.getElementType() != JetNodeTypes.BLOCK) {

            // For a single statement if 'for'
            childIndent = Indent.getNormalIndent();
        }
        else if (child.getElementType() == JetNodeTypes.WHEN_ENTRY) {
            // For the entry in when
            // TODO: Add an option for configuration?
            childIndent = Indent.getNormalIndent();
        }
        else if (childParent != null && childParent.getElementType() == JetNodeTypes.WHEN_ENTRY) {
            ASTNode prev = getPrevWithoutWhitespace(child);
            if (prev != null && prev.getText().equals("->")) {
                childIndent = indentIfNotBrace(child);
            }
        }
        else if (STATEMENT_PARTS.contains(myNode.getElementType()) && child.getElementType() != JetNodeTypes.BLOCK) {
            childIndent = Indent.getNormalIndent();
        }
        else if (childParent != null && childParent.getElementType() == JetNodeTypes.DOT_QUALIFIED_EXPRESSION) {
            if (childParent.getFirstChildNode() != child && childParent.getLastChildNode() != child) {
                childIndent = Indent.getContinuationWithoutFirstIndent(false);
            }
        }
        else if (childParent != null && childParent.getElementType() == JetNodeTypes.VALUE_PARAMETER_LIST) {
            String childText = child.getText();

            if (!(childText.equals("(") || childText.equals(")"))) {
                childIndent = Indent.getContinuationWithoutFirstIndent(false);
            }
        }
        else if (childParent != null && childParent.getElementType() == JetNodeTypes.TYPE_PARAMETER_LIST) {
            String childText = child.getText();

            if (!(childText.equals("<") || childText.equals(">"))) {
                childIndent = Indent.getContinuationWithoutFirstIndent(false);
            }
        }

        return new JetBlock(child, childAlignment, childIndent, wrap, mySettings, mySpacingBuilder);
    }

    private static Indent indentIfNotBrace(@NotNull ASTNode child) {
        return child.getElementType() == JetTokens.RBRACE || child.getElementType() == JetTokens.LBRACE
                ? Indent.getNoneIndent()
                : Indent.getNormalIndent();
    }

    private static ASTNode getPrevWithoutWhitespace(ASTNode node) {
        node = node.getTreePrev();
        while (node != null && node.getElementType() == TokenType.WHITE_SPACE) {
            node = node.getTreePrev();
        }

        return node;
    }

    @Override
    public Spacing getSpacing(Block child1, Block child2) {
        return mySpacingBuilder.getSpacing(this, child1, child2);
    }
    
    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        final IElementType type = getNode().getElementType();
        if (CODE_BLOCKS.contains(type) ||
                type == JetNodeTypes.WHEN ||
                type == JetNodeTypes.IF ||
                type == JetNodeTypes.FOR ||
                type == JetNodeTypes.WHILE ||
                type == JetNodeTypes.DO_WHILE) {

            return new ChildAttributes(Indent.getNormalIndent(), null);
        }
        else if (type == JetNodeTypes.DOT_QUALIFIED_EXPRESSION) {
            return new ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null);
        }

        return new ChildAttributes(Indent.getNoneIndent(), null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }
}
