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
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.*;

/**
 * @author yole
 * @see Block for good JavaDoc documentation
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

        Map<ASTNode, Alignment> childrenAlignments = createChildrenAlignments();

        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType childType = child.getElementType();

            if (child.getTextRange().getLength() == 0) continue;

            if (childType == TokenType.WHITE_SPACE) {
                continue;
            }

            Alignment childAlignment = childrenAlignments.containsKey(child) ? childrenAlignments.get(child) : null;
            blocks.add(buildSubBlock(child, childAlignment));
        }
        return Collections.unmodifiableList(blocks);
    }

    @NotNull
    private Block buildSubBlock(@NotNull ASTNode child, Alignment childAlignment) {
        Wrap wrap = null;

        // Affects to spaces around operators...
        if (child.getElementType() == JetNodeTypes.OPERATION_REFERENCE) {
            ASTNode operationNode = child.getFirstChildNode();
            if (operationNode != null) {
                return new JetBlock(operationNode, childAlignment, Indent.getNoneIndent(), wrap, mySettings, mySpacingBuilder);
            }
        }

        return new JetBlock(child, childAlignment, createChildIndent(child), wrap, mySettings, mySpacingBuilder);
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

    @NotNull
    protected Map<ASTNode, Alignment> createChildrenAlignments() {
        CommonCodeStyleSettings jetCommonSettings = mySettings.getCommonSettings(JetLanguage.INSTANCE);

        // Prepare strategies for setting up alignments - no alignment should be set by default
        AlignmentStrategy[] strategies = new AlignmentStrategy[] { AlignmentStrategy.getNullStrategy() };

        // Redefine list of strategies for some special elements
        IElementType parentType = myNode.getElementType();
        if (parentType == JetNodeTypes.VALUE_PARAMETER_LIST) {
            strategies = getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS, JetNodeTypes.VALUE_PARAMETER, JetTokens.COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, JetTokens.LPAR, JetTokens.RPAR);
        }
        else if (parentType == JetNodeTypes.VALUE_ARGUMENT_LIST) {
            strategies = getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, JetNodeTypes.VALUE_ARGUMENT, JetTokens.COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, JetTokens.LPAR, JetTokens.RPAR);
        }

        // Construct information about children alignment
        HashMap<ASTNode, Alignment> result = new HashMap<ASTNode, Alignment>();

        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType childType = child.getElementType();

            if (child.getTextRange().getLength() == 0) continue;

            if (childType == TokenType.WHITE_SPACE) {
                continue;
            }

            for (AlignmentStrategy strategy : strategies) {
                Alignment childAlignment = strategy.getAlignment(parentType, child.getElementType());
                if (childAlignment != null) {
                    result.put(child, childAlignment);
                    break;
                }
            }
        }

        return result;
    }

    private static AlignmentStrategy[] getAlignmentForChildInParenthesis(
            boolean shouldAlignChild, IElementType parameter, IElementType delimiter,
            boolean shouldAlignParenthesis, IElementType openParenth, IElementType closeParenth
    ) {
        return new AlignmentStrategy[] {
                AlignmentStrategy.wrap(shouldAlignChild ? Alignment.createAlignment() : null, false, parameter),
                AlignmentStrategy.wrap(shouldAlignChild ? Alignment.createAlignment() : null, false, delimiter),
                AlignmentStrategy.wrap(shouldAlignParenthesis ? Alignment.createAlignment() : null, false, openParenth, closeParenth)
        };
    }

    @Nullable
    protected Indent createChildIndent(@NotNull ASTNode child) {
        ASTNode childParent = child.getTreeParent();

        if (CODE_BLOCKS.contains(myNode.getElementType())) {
            return indentIfNotBrace(child);
        }

        if (childParent != null &&
                 childParent.getElementType() == JetNodeTypes.BODY &&
                 child.getElementType() != JetNodeTypes.BLOCK) {

            // For a single statement if 'for'
            return Indent.getNormalIndent();
        }

        if (child.getElementType() == JetNodeTypes.WHEN_ENTRY) {
            // For the entry in when
            // TODO: Add an option for configuration?
            return Indent.getNormalIndent();
        }

        if (childParent != null && childParent.getElementType() == JetNodeTypes.WHEN_ENTRY) {
            ASTNode prev = getPrevWithoutWhitespace(child);
            if (prev != null && prev.getText().equals("->")) {
                return indentIfNotBrace(child);
            }
        }

        if (STATEMENT_PARTS.contains(myNode.getElementType()) && child.getElementType() != JetNodeTypes.BLOCK) {
            return Indent.getNormalIndent();
        }

        if (childParent != null && childParent.getElementType() == JetNodeTypes.DOT_QUALIFIED_EXPRESSION) {
            if (childParent.getFirstChildNode() != child && childParent.getLastChildNode() != child) {
                return Indent.getContinuationWithoutFirstIndent(false);
            }
        }

        if (childParent != null &&
                 (childParent.getElementType() == JetNodeTypes.VALUE_PARAMETER_LIST ||
                  childParent.getElementType() == JetNodeTypes.TYPE_PARAMETER_LIST ||
                  childParent.getElementType() == JetNodeTypes.VALUE_ARGUMENT_LIST ||
                  childParent.getElementType() == JetNodeTypes.TYPE_ARGUMENT_LIST)) {
            if (child.getElementType() == JetTokens.RPAR) {
                return Indent.getNoneIndent();
            }

            return Indent.getContinuationWithoutFirstIndent(false);
        }

        return Indent.getNoneIndent();
    }
}
