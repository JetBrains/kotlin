/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.kdoc.lexer.KDocTokens;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @see Block for good JavaDoc documentation
 */
public class JetBlock extends AbstractBlock {
    private static final int KDOC_COMMENT_INDENT = 1;
    private final NodeAlignmentStrategy myAlignmentStrategy;
    private final Indent myIndent;
    private final CodeStyleSettings mySettings;
    private final KotlinSpacingBuilder mySpacingBuilder;

    private List<Block> mySubBlocks;

    private static final TokenSet CODE_BLOCKS = TokenSet.create(
            BLOCK,
            CLASS_BODY,
            FUNCTION_LITERAL);

    // private static final List<IndentWhitespaceRule>

    public JetBlock(
            @NotNull ASTNode node,
            NodeAlignmentStrategy alignmentStrategy,
            Indent indent,
            Wrap wrap,
            CodeStyleSettings settings,
            KotlinSpacingBuilder spacingBuilder
    ) {
        super(node, wrap, alignmentStrategy.getAlignment(node));
        myAlignmentStrategy = alignmentStrategy;
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

        NodeAlignmentStrategy childrenAlignmentStrategy = getChildrenAlignmentStrategy();

        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType childType = child.getElementType();

            if (child.getTextRange().getLength() == 0) continue;

            if (childType == TokenType.WHITE_SPACE) {
                continue;
            }

            blocks.add(buildSubBlock(child, childrenAlignmentStrategy));
        }
        return Collections.unmodifiableList(blocks);
    }

    @NotNull
    private Block buildSubBlock(@NotNull ASTNode child, NodeAlignmentStrategy alignmentStrategy) {
        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.getElementType() == OPERATION_REFERENCE) {
            ASTNode operationNode = child.getFirstChildNode();
            if (operationNode != null) {
                return new JetBlock(operationNode, alignmentStrategy, createChildIndent(child), null, mySettings, mySpacingBuilder);
            }
        }

        return new JetBlock(child, alignmentStrategy, createChildIndent(child), null, mySettings, mySpacingBuilder);
    }

    private static ASTNode getPrevWithoutWhitespace(ASTNode node) {
        node = node.getTreePrev();
        while (node != null && node.getElementType() == TokenType.WHITE_SPACE) {
            node = node.getTreePrev();
        }

        return node;
    }

    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return mySpacingBuilder.getSpacing(this, child1, child2);
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        IElementType type = getNode().getElementType();
        if (CODE_BLOCKS.contains(type) ||
            type == WHEN ||
            type == IF ||
            type == FOR ||
            type == WHILE ||
            type == DO_WHILE) {

            return new ChildAttributes(Indent.getNormalIndent(), null);
        }
        else if (type == TRY) {
            // In try - try BLOCK catch BLOCK finally BLOCK
            return new ChildAttributes(Indent.getNoneIndent(), null);
        }
        else if (type == DOT_QUALIFIED_EXPRESSION || type == SAFE_ACCESS_EXPRESSION) {
            return new ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null);
        }
        else if (type == VALUE_PARAMETER_LIST || type == VALUE_ARGUMENT_LIST) {
            // Child index 1 - cursor is after ( - parameter alignment should be recreated
            // Child index 0 - before expression - know nothing about it
            if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < getSubBlocks().size()) {
                Block block = getSubBlocks().get(newChildIndex);
                return new ChildAttributes(block.getIndent(), block.getAlignment());
            }
            return new ChildAttributes(Indent.getContinuationIndent(), null);
        }
        else if (type == DOC_COMMENT) {
            return new ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null);
        }

        if (type == PARENTHESIZED) {
            return super.getChildAttributes(newChildIndex);
        }

        List<Block> blocks = getSubBlocks();
        if (newChildIndex != 0) {
            boolean isIncomplete = newChildIndex < blocks.size() ? blocks.get(newChildIndex - 1).isIncomplete() : isIncomplete();
            if (isIncomplete) {
                return super.getChildAttributes(newChildIndex);
            }
        }

        return new ChildAttributes(Indent.getNoneIndent(), null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    private NodeAlignmentStrategy getChildrenAlignmentStrategy() {
        final CommonCodeStyleSettings jetCommonSettings = mySettings.getCommonSettings(JetLanguage.INSTANCE);
        JetCodeStyleSettings jetSettings = mySettings.getCustomSettings(JetCodeStyleSettings.class);

        // Redefine list of strategies for some special elements
        IElementType parentType = myNode.getElementType();
        if (parentType == VALUE_PARAMETER_LIST) {
            return getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS, VALUE_PARAMETER, COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR);
        }
        else if (parentType == VALUE_ARGUMENT_LIST) {
            return getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, VALUE_ARGUMENT, COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR);
        }
        else if (parentType == WHEN) {
            return getAlignmentForCaseBranch(jetSettings.ALIGN_IN_COLUMNS_CASE_BRANCH);
        }
        else if (parentType == BINARY_EXPRESSION) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, getAlignment())));
        }
        else if (parentType == DELEGATION_SPECIFIER_LIST) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST, getAlignment())));
        }
        else if (parentType == PARENTHESIZED) {
            return new NodeAlignmentStrategy() {
                Alignment bracketsAlignment = jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION ? Alignment.createAlignment() : null;

                @Nullable
                @Override
                public Alignment getAlignment(@NotNull ASTNode childNode) {
                    IElementType childNodeType = childNode.getElementType();
                    ASTNode prev = getPrevWithoutWhitespace(childNode);

                    if ((prev != null && prev.getElementType() == TokenType.ERROR_ELEMENT) || childNodeType == TokenType.ERROR_ELEMENT) {
                        return bracketsAlignment;
                    }

                    if (childNodeType == LPAR || childNodeType == RPAR) {
                        return bracketsAlignment;
                    }

                    return null;
                }
            };
        }

        return myAlignmentStrategy;
    }

    private static NodeAlignmentStrategy getAlignmentForChildInParenthesis(
            boolean shouldAlignChild, final IElementType parameter, final IElementType delimiter,
            boolean shouldAlignParenthesis, final IElementType openBracket, final IElementType closeBracket
    ) {
        final Alignment parameterAlignment = shouldAlignChild ? Alignment.createAlignment() : null;
        final Alignment bracketsAlignment = shouldAlignParenthesis ? Alignment.createAlignment() : null;

        return new NodeAlignmentStrategy() {
            @Override
            public Alignment getAlignment(@NotNull ASTNode node) {
                IElementType childNodeType = node.getElementType();

                ASTNode prev = getPrevWithoutWhitespace(node);
                if ((prev != null && prev.getElementType() == TokenType.ERROR_ELEMENT) || childNodeType == TokenType.ERROR_ELEMENT) {
                    // Prefer align to parameters on incomplete code (case of line break after comma, when next parameters is absent)
                    return parameterAlignment;
                }

                if (childNodeType == openBracket || childNodeType == closeBracket) {
                    return bracketsAlignment;
                }

                if (childNodeType == parameter || childNodeType == delimiter) {
                    return parameterAlignment;
                }

                return null;
            }
        };
    }

    private static NodeAlignmentStrategy getAlignmentForCaseBranch(boolean shouldAlignInColumns) {
        if (shouldAlignInColumns) {
            return NodeAlignmentStrategy.fromTypes(
                    AlignmentStrategy.createAlignmentPerTypeStrategy(Arrays.asList((IElementType) ARROW), WHEN_ENTRY, true));
        }
        else {
            return NodeAlignmentStrategy.getNullStrategy();
        }
    }

    static ASTIndentStrategy[] INDENT_RULES = new ASTIndentStrategy[] {
            ASTIndentStrategy.forNode("No indent for braces in blocks")
                    .in(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
                    .forType(RBRACE, LBRACE)
                    .set(Indent.getNoneIndent()),

            ASTIndentStrategy.forNode("Indent for block content")
                    .in(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
                    .notForType(RBRACE, LBRACE, BLOCK)
                    .set(Indent.getNormalIndent()),

            ASTIndentStrategy.forNode("Indent for property accessors")
                    .in(PROPERTY)
                    .forType(PROPERTY_ACCESSOR)
                    .set(Indent.getNormalIndent()),

            ASTIndentStrategy.forNode("For a single statement in 'for'")
                    .in(BODY)
                    .notForType(BLOCK)
                    .set(Indent.getNormalIndent()),

            ASTIndentStrategy.forNode("For the entry in when")
                    .forType(WHEN_ENTRY)
                    .set(Indent.getNormalIndent()),

            ASTIndentStrategy.forNode("For single statement in THEN and ELSE")
                    .in(THEN, ELSE)
                    .notForType(BLOCK)
                    .set(Indent.getNormalIndent()),

            ASTIndentStrategy.forNode("Indent for parts")
                    .in(PROPERTY, FUN)
                    .notForType(BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD)
                    .set(Indent.getContinuationWithoutFirstIndent()),

            ASTIndentStrategy.forNode("Chained calls")
                    .in(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)
                    .set(Indent.getContinuationWithoutFirstIndent(true)),

            ASTIndentStrategy.forNode("Delegation list")
                    .in(DELEGATION_SPECIFIER_LIST)
                    .set(Indent.getContinuationIndent(false)),

            ASTIndentStrategy.forNode("Binary expressions")
                    .in(BINARY_EXPRESSION)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            ASTIndentStrategy.forNode("Parenthesized expression")
                    .in(PARENTHESIZED)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            ASTIndentStrategy.forNode("KDoc comment indent")
                    .in(DOC_COMMENT)
                    .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
                    .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

            ASTIndentStrategy.forNode("Block in when entry")
                    .in(WHEN_ENTRY)
                    .notForType(BLOCK, WHEN_CONDITION_EXPRESSION, WHEN_CONDITION_IN_RANGE, WHEN_CONDITION_IS_PATTERN, ELSE_KEYWORD, ARROW)
                    .set(Indent.getNormalIndent()),
    };

    @Nullable
    protected static Indent createChildIndent(@NotNull ASTNode child) {
        ASTNode childParent = child.getTreeParent();
        IElementType childType = child.getElementType();

        // SCRIPT: Avoid indenting script top BLOCK contents
        if (childParent != null && childParent.getTreeParent() != null) {
            if (childParent.getElementType() == BLOCK && childParent.getTreeParent().getElementType() == SCRIPT) {
                return Indent.getNoneIndent();
            }
        }

        for (ASTIndentStrategy strategy : INDENT_RULES) {
            Indent indent = strategy.getIndent(child);
            if (indent != null) {
                return indent;
            }
        }

        // TODO: Try to rewrite other rules to declarative style
        if (childParent != null) {
            IElementType parentType = childParent.getElementType();

            if (parentType == VALUE_PARAMETER_LIST || parentType == VALUE_ARGUMENT_LIST) {
                ASTNode prev = getPrevWithoutWhitespace(child);
                if (childType == RPAR && (prev == null || prev.getElementType() != TokenType.ERROR_ELEMENT)) {
                    return Indent.getNoneIndent();
                }

                return Indent.getContinuationWithoutFirstIndent();
            }

            if (parentType == TYPE_PARAMETER_LIST || parentType == TYPE_ARGUMENT_LIST) {
                return Indent.getContinuationWithoutFirstIndent();
            }
        }

        return Indent.getNoneIndent();
    }

    @Nullable
    private static Alignment createAlignment(boolean alignOption, @Nullable Alignment defaultAlignment) {
        return alignOption ? createAlignmentOrDefault(null, defaultAlignment) : defaultAlignment;
    }

    @Nullable
    private static Alignment createAlignmentOrDefault(@Nullable Alignment base, @Nullable Alignment defaultAlignment) {
        if (defaultAlignment == null) {
            return base == null ? Alignment.createAlignment() : Alignment.createChildAlignment(base);
        }
        return defaultAlignment;
    }
}
