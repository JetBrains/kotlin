/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.*;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @see Block for good JavaDoc documentation
 */
public class JetBlock extends AbstractBlock {
    private final ASTAlignmentStrategy myAlignmentStrategy;
    private final Indent myIndent;
    private final CodeStyleSettings mySettings;
    private final SpacingBuilder mySpacingBuilder;

    private List<Block> mySubBlocks;

    private static final TokenSet CODE_BLOCKS = TokenSet.create(
            BLOCK,
            CLASS_BODY,
            FUNCTION_LITERAL);

    // private static final List<IndentWhitespaceRule>

    public JetBlock(@NotNull ASTNode node,
            ASTAlignmentStrategy alignmentStrategy,
            Indent indent,
            Wrap wrap,
            CodeStyleSettings settings,
            SpacingBuilder spacingBuilder) {

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

        ASTAlignmentStrategy childrenAlignmentStrategy = getChildrenAlignmentStrategy();

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
    private Block buildSubBlock(@NotNull ASTNode child, ASTAlignmentStrategy alignmentStrategy) {
        Wrap wrap = null;

        // Affects to spaces around operators...
        if (child.getElementType() == OPERATION_REFERENCE) {
            ASTNode operationNode = child.getFirstChildNode();
            if (operationNode != null) {
                return new JetBlock(operationNode, alignmentStrategy, Indent.getNoneIndent(), wrap, mySettings, mySpacingBuilder);
            }
        }

        return new JetBlock(child, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder);
    }

    private static Indent indentIfNotBrace(@NotNull ASTNode child) {
        return child.getElementType() == RBRACE || child.getElementType() == LBRACE
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
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        Spacing spacing = mySpacingBuilder.getSpacing(this, child1, child2);
        if (spacing != null) {
            return spacing;
        }

        // TODO: extend SpacingBuilder API - afterInside(RBRACE, FUNCTION_LITERAL).spacing(...), beforeInside(RBRACE, FUNCTION_LITERAL).spacing(...)
        if (!(child1 instanceof ASTBlock && child2 instanceof ASTBlock)) {
            return null;
        }

        IElementType parentType = this.getNode().getElementType();
        IElementType child1Type = ((ASTBlock) child1).getNode().getElementType();
        IElementType child2Type = ((ASTBlock) child2).getNode().getElementType();

        JetCodeStyleSettings jetSettings = mySettings.getCustomSettings(JetCodeStyleSettings.class);
        int spacesInSimpleMethod = jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD ? 1 : 0;

        if (parentType == FUNCTION_LITERAL && child1Type == LBRACE && child2Type == BLOCK) {
            return Spacing.createDependentLFSpacing(
                    spacesInSimpleMethod, spacesInSimpleMethod, this.getTextRange(),
                    mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
        }

        if (parentType == FUNCTION_LITERAL && child1Type == ARROW && child2Type == BLOCK) {
            return Spacing.createDependentLFSpacing(1, 1, this.getTextRange(), mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
        }

        if (parentType == FUNCTION_LITERAL && child2Type == RBRACE) {
            return Spacing.createDependentLFSpacing(
                    spacesInSimpleMethod, spacesInSimpleMethod, this.getTextRange(),
                    mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
        }

        if (parentType == FUNCTION_LITERAL && child1Type == LBRACE) {
            if (child2Type == VALUE_PARAMETER_LIST) {
                ASTNode firstParamListNode = ((ASTBlock) child2).getNode().getFirstChildNode();
                if (firstParamListNode != null && firstParamListNode.getElementType() == LPAR) {
                    // Don't put space for situation {<here>(a: Int) -> a }
                    return Spacing.createSpacing(0, 0, 0, mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
                }
            }

            return Spacing.createSpacing(spacesInSimpleMethod, spacesInSimpleMethod, 0,
                                         mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
        }

        return null;
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(int newChildIndex) {
        final IElementType type = getNode().getElementType();
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
        else if (type == DOT_QUALIFIED_EXPRESSION) {
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

        if (isIncomplete()) {
            return super.getChildAttributes(newChildIndex);
        }

        return new ChildAttributes(Indent.getNoneIndent(), null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    private ASTAlignmentStrategy getChildrenAlignmentStrategy() {
        CommonCodeStyleSettings jetCommonSettings = mySettings.getCommonSettings(JetLanguage.INSTANCE);
        JetCodeStyleSettings jetSettings = mySettings.getCustomSettings(JetCodeStyleSettings.class);

        // Prepare default null strategy
        ASTAlignmentStrategy strategy = myAlignmentStrategy;

        // Redefine list of strategies for some special elements
        IElementType parentType = myNode.getElementType();
        if (parentType == VALUE_PARAMETER_LIST) {
            strategy = getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS, VALUE_PARAMETER, COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR);
        }
        else if (parentType == VALUE_ARGUMENT_LIST) {
            strategy = getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, VALUE_ARGUMENT, COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR);
        }
        else if (parentType == WHEN) {
            strategy = getAlignmentForCaseBranch(jetSettings.ALIGN_IN_COLUMNS_CASE_BRANCH);
        }
        return strategy;
    }

    private static ASTAlignmentStrategy getAlignmentForChildInParenthesis(
            boolean shouldAlignChild, final IElementType parameter, final IElementType delimiter,
            boolean shouldAlignParenthesis, final IElementType openParenth, final IElementType closeParenth
    ) {
        // TODO: Check this approach in other situations and refactor
        final Alignment parameterAlignment = shouldAlignChild ? Alignment.createAlignment() : null;
        final Alignment parenthesisAlignment = shouldAlignParenthesis ? Alignment.createAlignment() : null;

        return new ASTAlignmentStrategy() {
            @Override
            public Alignment getAlignment(ASTNode node) {
                IElementType childNodeType = node.getElementType();

                ASTNode prev = getPrevWithoutWhitespace(node);
                if ((prev != null && prev.getElementType() == TokenType.ERROR_ELEMENT) || childNodeType == TokenType.ERROR_ELEMENT) {
                    return parameterAlignment;
                }

                if (childNodeType == openParenth || childNodeType == closeParenth) {
                    return parenthesisAlignment;
                }

                if (childNodeType == parameter || childNodeType == delimiter) {
                    return parameterAlignment;
                }

                return null;
            }
        };
    }

    private static ASTAlignmentStrategy getAlignmentForCaseBranch(boolean shouldAlignInColumns) {
        if (shouldAlignInColumns) {
            return ASTAlignmentStrategy
                    .fromTypes(AlignmentStrategy.createAlignmentPerTypeStrategy(Arrays.asList((IElementType) ARROW), WHEN_ENTRY, true));
        }
        else {
            return ASTAlignmentStrategy.getNullStrategy();
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

            ASTIndentStrategy.forNode("For a single statement if 'for'")
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
                    .notForType(BLOCK)
                    .set(Indent.getContinuationWithoutFirstIndent()),
    };

    @Nullable
    protected static Indent createChildIndent(@NotNull ASTNode child) {
        ASTNode childParent = child.getTreeParent();
        IElementType childType = child.getElementType();

        for (ASTIndentStrategy strategy : INDENT_RULES) {
            Indent indent = strategy.getIndent(child);
            if (indent != null) {
                return indent;
            }
        }

        // TODO: Try to rewrite other rules to declarative style

        if (childParent != null && childParent.getElementType() == WHEN_ENTRY) {
            ASTNode prev = getPrevWithoutWhitespace(child);
            if (prev != null && prev.getText().equals("->")) {
                return indentIfNotBrace(child);
            }
        }

        if (childParent != null && childParent.getElementType() == DOT_QUALIFIED_EXPRESSION) {
            if (childParent.getFirstChildNode() != child && childParent.getLastChildNode() != child) {
                return Indent.getContinuationWithoutFirstIndent(false);
            }
        }

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
}
