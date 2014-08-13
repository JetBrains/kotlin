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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class JetHighlightingColors {
    // default keys (mostly syntax elements)
    public static final TextAttributesKey KEYWORD = createTextAttributesKey("KOTLIN_KEYWORD", JavaHighlightingColors.KEYWORD);
    public static final TextAttributesKey BUILTIN_ANNOTATION = createTextAttributesKey("KOTLIN_BUILTIN_ANNOTATION", JavaHighlightingColors.KEYWORD);
    public static final TextAttributesKey NUMBER = createTextAttributesKey("KOTLIN_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING = createTextAttributesKey("KOTLIN_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey STRING_ESCAPE = createTextAttributesKey("KOTLIN_STRING_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    public static final TextAttributesKey INVALID_STRING_ESCAPE = createTextAttributesKey("KOTLIN_INVALID_STRING_ESCAPE", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
    public static final TextAttributesKey OPERATOR_SIGN = createTextAttributesKey("KOTLIN_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey PARENTHESIS = createTextAttributesKey("KOTLIN_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BRACES = createTextAttributesKey("KOTLIN_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey BRACKETS = createTextAttributesKey("KOTLIN_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey FUNCTION_LITERAL_BRACES_AND_ARROW = createTextAttributesKey("KOTLIN_FUNCTION_LITERAL_BRACES_AND_ARROW");
    public static final TextAttributesKey COMMA = createTextAttributesKey("KOTLIN_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey SEMICOLON = createTextAttributesKey("KOTLIN_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey DOT = createTextAttributesKey("KOTLIN_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey SAFE_ACCESS = createTextAttributesKey("KOTLIN_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey ARROW = createTextAttributesKey("KOTLIN_ARROW", PARENTHESIS);
    public static final TextAttributesKey LINE_COMMENT = createTextAttributesKey("KOTLIN_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT = createTextAttributesKey("KOTLIN_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey DOC_COMMENT = createTextAttributesKey("KOTLIN_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
    public static final TextAttributesKey KDOC_TAG = createTextAttributesKey("KDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);
    public static final TextAttributesKey KDOC_TAG_VALUE = createTextAttributesKey("KDOC_TAG_VALUE", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE);

    // class kinds
    public static final TextAttributesKey CLASS = createTextAttributesKey("KOTLIN_CLASS", CodeInsightColors.CLASS_NAME_ATTRIBUTES);
    public static final TextAttributesKey TYPE_PARAMETER = createTextAttributesKey("KOTLIN_TYPE_PARAMETER", CodeInsightColors.TYPE_PARAMETER_NAME_ATTRIBUTES);
    public static final TextAttributesKey ABSTRACT_CLASS = createTextAttributesKey("KOTLIN_ABSTRACT_CLASS", CodeInsightColors.ABSTRACT_CLASS_NAME_ATTRIBUTES);
    public static final TextAttributesKey TRAIT = createTextAttributesKey("KOTLIN_TRAIT", CodeInsightColors.INTERFACE_NAME_ATTRIBUTES);
    public static final TextAttributesKey ANNOTATION = createTextAttributesKey("KOTLIN_ANNOTATION", CodeInsightColors.ANNOTATION_NAME_ATTRIBUTES);
    public static final TextAttributesKey OBJECT = createTextAttributesKey("KOTLIN_OBJECT", CLASS);

    // variable kinds
    public static final TextAttributesKey MUTABLE_VARIABLE = createTextAttributesKey("KOTLIN_MUTABLE_VARIABLE");
    public static final TextAttributesKey LOCAL_VARIABLE = createTextAttributesKey("KOTLIN_LOCAL_VARIABLE", CodeInsightColors.LOCAL_VARIABLE_ATTRIBUTES);
    public static final TextAttributesKey PARAMETER = createTextAttributesKey("KOTLIN_PARAMETER", CodeInsightColors.PARAMETER_ATTRIBUTES);
    public static final TextAttributesKey WRAPPED_INTO_REF = createTextAttributesKey("KOTLIN_WRAPPED_INTO_REF", CodeInsightColors.IMPLICIT_ANONYMOUS_CLASS_PARAMETER_ATTRIBUTES);
    public static final TextAttributesKey INSTANCE_PROPERTY = createTextAttributesKey("KOTLIN_INSTANCE_PROPERTY", CodeInsightColors.INSTANCE_FIELD_ATTRIBUTES);
    public static final TextAttributesKey PACKAGE_PROPERTY = createTextAttributesKey("KOTLIN_PACKAGE_PROPERTY", CodeInsightColors.STATIC_FIELD_ATTRIBUTES);
    public static final TextAttributesKey PROPERTY_WITH_BACKING_FIELD = createTextAttributesKey("KOTLIN_PROPERTY_WITH_BACKING_FIELD");
    public static final TextAttributesKey BACKING_FIELD_ACCESS = createTextAttributesKey("KOTLIN_BACKING_FIELD_ACCESS");
    public static final TextAttributesKey EXTENSION_PROPERTY = createTextAttributesKey("KOTLIN_EXTENSION_PROPERTY");

    // functions
    public static final TextAttributesKey FUNCTION_LITERAL_DEFAULT_PARAMETER = createTextAttributesKey("KOTLIN_CLOSURE_DEFAULT_PARAMETER");
    public static final TextAttributesKey FUNCTION_DECLARATION = createTextAttributesKey("KOTLIN_FUNCTION_DECLARATION", CodeInsightColors.METHOD_DECLARATION_ATTRIBUTES);
    public static final TextAttributesKey FUNCTION_CALL = createTextAttributesKey("KOTLIN_FUNCTION_CALL", CodeInsightColors.METHOD_CALL_ATTRIBUTES);
    public static final TextAttributesKey PACKAGE_FUNCTION_CALL = createTextAttributesKey("KOTLIN_PACKAGE_FUNCTION_CALL", CodeInsightColors.STATIC_METHOD_ATTRIBUTES);
    public static final TextAttributesKey EXTENSION_FUNCTION_CALL = createTextAttributesKey("KOTLIN_EXTENSION_FUNCTION_CALL");
    public static final TextAttributesKey CONSTRUCTOR_CALL = createTextAttributesKey("KOTLIN_CONSTRUCTOR", CodeInsightColors.CONSTRUCTOR_CALL_ATTRIBUTES);
    public static final TextAttributesKey VARIABLE_AS_FUNCTION_CALL = createTextAttributesKey("KOTLIN_VARIABLE_AS_FUNCTION");
    public static final TextAttributesKey VARIABLE_AS_FUNCTION_LIKE_CALL = createTextAttributesKey("KOTLIN_VARIABLE_AS_FUNCTION_LIKE");

    // other
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("KOTLIN_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
    public static final TextAttributesKey AUTO_CASTED_VALUE = createTextAttributesKey("KOTLIN_AUTO_CASTED_VALUE");
    public static final TextAttributesKey LABEL = createTextAttributesKey("KOTLIN_LABEL");
    public static final TextAttributesKey DEBUG_INFO = createTextAttributesKey("KOTLIN_DEBUG_INFO");
    public static final TextAttributesKey RESOLVED_TO_ERROR = createTextAttributesKey("KOTLIN_RESOLVED_TO_ERROR");

    private JetHighlightingColors() {
    }
}
