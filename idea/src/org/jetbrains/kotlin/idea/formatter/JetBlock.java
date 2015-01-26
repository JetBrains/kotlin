/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.formatting.*;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.JetDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.JetNodeTypes.*;
import static org.jetbrains.kotlin.idea.formatter.NodeIndentStrategy.strategy;
import static org.jetbrains.kotlin.lexer.JetTokens.*;

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

    private static final TokenSet BINARY_EXPRESSIONS = TokenSet.create(BINARY_EXPRESSION, BINARY_WITH_TYPE, IS_EXPRESSION);
    private static final TokenSet QUALIFIED_OPERATION = TokenSet.create(DOT, SAFE_ACCESS);
    private static final TokenSet ALIGN_FOR_BINARY_OPERATIONS =
            TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, ELVIS, LT, GT, LTEQ, GTEQ, ANDAND, OROR);

    private static final TokenSet CODE_BLOCKS = TokenSet.create(
            BLOCK,
            CLASS_BODY,
            FUNCTION_LITERAL);

    private static final TokenSet KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC, KDocElementTypes.KDOC_SECTION);

    // private static final List<IndentWhitespaceRule>

    public JetBlock(
            @NotNull ASTNode node,
            @NotNull NodeAlignmentStrategy alignmentStrategy,
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
            List<Block> nodeSubBlocks = buildSubBlocks();

            if (getNode().getElementType() == DOT_QUALIFIED_EXPRESSION || getNode().getElementType() == SAFE_ACCESS_EXPRESSION) {
                int operationBlockIndex = findNodeBlockIndex(nodeSubBlocks, QUALIFIED_OPERATION);
                if (operationBlockIndex != -1) {
                    // Create fake ".something" or "?.something" block here, so child indentation will be
                    // relative to it when it starts from new line (see Indent javadoc).

                    Block operationBlock = nodeSubBlocks.get(operationBlockIndex);
                    SyntheticKotlinBlock operationSynteticBlock =
                            new SyntheticKotlinBlock(
                                    ((ASTBlock) operationBlock).getNode(),
                                    nodeSubBlocks.subList(operationBlockIndex, nodeSubBlocks.size()),
                                    null, operationBlock.getIndent(), null, mySpacingBuilder);

                    nodeSubBlocks = ContainerUtil.addAll(
                            ContainerUtil.newArrayList(nodeSubBlocks.subList(0, operationBlockIndex)),
                            operationSynteticBlock);
                }
            }

            mySubBlocks = nodeSubBlocks;
        }
        return mySubBlocks;
    }

    private List<Block> buildSubBlocks() {
        List<Block> blocks = new ArrayList<Block>();

        NodeAlignmentStrategy childrenAlignmentStrategy = getChildrenAlignmentStrategy();
        WrappingStrategy wrappingStrategy = getWrappingStrategy();

        for (ASTNode child = myNode.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType childType = child.getElementType();

            if (child.getTextRange().getLength() == 0) continue;

            if (childType == TokenType.WHITE_SPACE) {
                continue;
            }

            blocks.add(buildSubBlock(child, childrenAlignmentStrategy, wrappingStrategy));
        }

        return Collections.unmodifiableList(blocks);
    }

    @NotNull
    private Block buildSubBlock(
            @NotNull ASTNode child,
            NodeAlignmentStrategy alignmentStrategy,
            @NotNull WrappingStrategy wrappingStrategy) {
        Wrap wrap = wrappingStrategy.getWrap(child.getElementType());

        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.getElementType() == OPERATION_REFERENCE) {
            ASTNode operationNode = child.getFirstChildNode();
            if (operationNode != null) {
                return new JetBlock(operationNode, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder);
            }
        }

        return new JetBlock(child, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder);
    }

    private static ASTNode getPrevWithoutWhitespace(ASTNode node) {
        node = node.getTreePrev();
        while (node != null && node.getElementType() == TokenType.WHITE_SPACE) {
            node = node.getTreePrev();
        }

        return node;
    }

    private static ASTNode getPrevWithoutWhitespaceAndComments(ASTNode node) {
        node = node.getTreePrev();
        while (node != null && (node.getElementType() == TokenType.WHITE_SPACE || JetTokens.COMMENTS.contains(node.getElementType()))) {
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

    @NotNull
    private static WrappingStrategy getWrappingStrategyForItemList(int wrapType, @NotNull final IElementType itemType) {
        final Wrap itemWrap = Wrap.createWrap(wrapType, false);
        return new WrappingStrategy() {
            @Nullable
            @Override
            public Wrap getWrap(@NotNull IElementType childElementType) {
                return childElementType == itemType ? itemWrap : null;
            }
        };
    }

    @NotNull
    private WrappingStrategy getWrappingStrategy() {
        CommonCodeStyleSettings commonSettings = mySettings.getCommonSettings(JetLanguage.INSTANCE);
        IElementType elementType = myNode.getElementType();

        if (elementType == VALUE_ARGUMENT_LIST) {
            return getWrappingStrategyForItemList(commonSettings.CALL_PARAMETERS_WRAP, VALUE_ARGUMENT);
        }
        if (elementType == VALUE_PARAMETER_LIST) {
            IElementType parentElementType = myNode.getTreeParent().getElementType();
            if (parentElementType == FUN || parentElementType == CLASS) {
                return getWrappingStrategyForItemList(commonSettings.METHOD_PARAMETERS_WRAP, VALUE_PARAMETER);
            }
        }

        return WrappingStrategy.NoWrapping.INSTANCE$;
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
        else if (parentType == WHEN_ENTRY) {
            // Propagate when alignment for ->
            return myAlignmentStrategy;
        }
        else if (BINARY_EXPRESSIONS.contains(parentType) && ALIGN_FOR_BINARY_OPERATIONS.contains(getOperationType(getNode()))) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, getAlignment())));
        }
        else if (parentType == DELEGATION_SPECIFIER_LIST || parentType == INITIALIZER_LIST) {
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

        return NodeAlignmentStrategy.getNullStrategy();
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

    private static final NodeIndentStrategy[] INDENT_RULES = new NodeIndentStrategy[] {
            strategy("No indent for braces in blocks")
                    .in(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
                    .forType(RBRACE, LBRACE)
                    .set(Indent.getNoneIndent()),

            strategy("Indent for block content")
                    .in(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
                    .notForType(RBRACE, LBRACE, BLOCK)
                    .set(Indent.getNormalIndent(false)),

            strategy("Indent for property accessors")
                    .in(PROPERTY)
                    .forType(PROPERTY_ACCESSOR)
                    .set(Indent.getNormalIndent()),

            strategy("For a single statement in 'for'")
                    .in(BODY)
                    .notForType(BLOCK)
                    .set(Indent.getNormalIndent()),

            strategy("For the entry in when")
                    .forType(WHEN_ENTRY)
                    .set(Indent.getNormalIndent()),

            strategy("For single statement in THEN and ELSE")
                    .in(THEN, ELSE)
                    .notForType(BLOCK)
                    .set(Indent.getNormalIndent()),

            strategy("Indent for parts")
                    .in(PROPERTY, FUN, MULTI_VARIABLE_DECLARATION)
                    .notForType(BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD)
                    .set(Indent.getContinuationWithoutFirstIndent()),

            strategy("Chained calls")
                    .in(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            strategy("Delegation list")
                    .in(DELEGATION_SPECIFIER_LIST, INITIALIZER_LIST)
                    .set(Indent.getContinuationIndent(false)),

            strategy("Indices")
                    .in(INDICES)
                    .set(Indent.getContinuationIndent(false)),

            strategy("Binary expressions")
                    .in(BINARY_EXPRESSIONS)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            strategy("Parenthesized expression")
                    .in(PARENTHESIZED)
                    .set(Indent.getContinuationWithoutFirstIndent(false)),

            strategy("KDoc comment indent")
                    .in(KDOC_CONTENT)
                    .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
                    .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

            strategy("Block in when entry")
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

        // do not indent child after heading comments inside declaration
        if (childParent != null && childParent.getPsi() instanceof JetDeclaration) {
            ASTNode prev = getPrevWithoutWhitespace(child);
            if (prev != null && JetTokens.COMMENTS.contains(prev.getElementType()) && getPrevWithoutWhitespaceAndComments(prev) == null) {
                return Indent.getNoneIndent();
            }
        }

        for (NodeIndentStrategy strategy : INDENT_RULES) {
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

    private static int findNodeBlockIndex(List<Block> blocks, final TokenSet tokenSet) {
        return ContainerUtil.indexOf(blocks, new Condition<Block>() {
            @Override
            public boolean value(Block block) {
                if (!(block instanceof ASTBlock)) return false;

                ASTNode node = ((ASTBlock) block).getNode();
                return node != null && tokenSet.contains(node.getElementType());
            }
        });
    }

    @Nullable
    private static IElementType getOperationType(ASTNode node) {
        ASTNode operationNode = node.findChildByType(OPERATION_REFERENCE);
        return operationNode != null ? operationNode.getFirstChildNode().getElementType() : null;
    }
}
