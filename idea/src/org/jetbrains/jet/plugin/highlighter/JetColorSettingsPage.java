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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetIconProvider;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;

public class JetColorSettingsPage implements ColorSettingsPage {
    @Override
    public Icon getIcon() {
        return JetIconProvider.KOTLIN_ICON;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new JetHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "/* Block comment */\n" +
               "import kotlin.util.*\n" +
               "                               Bad characters: \\n #\n" +
               "val globalConst = 0\n" +
               "/**\n" +
               " * Doc comment here for `SomeClass`\n" +
               " * @see Iterator#next()\n" +
               " */\n" +
               "[Annotation]\n" +
               "public class SomeClass<out T : Runnable>(param : ATrait, reassignedParam : Array<Int>, val paramProperty: String?) { // some comment\n" +
               "  private field : T {\n" +
               "    return null\n" +
               "  }\n" +
               "  private open unusedField : Double = 12345.67890\n" +
               "  private anotherString : UnknownType = \"$field Another\\nStrin\\g\";\n" +
               "\n" +
               "  {\n" +
               "    paramProperty.?length ?: 33\n" +
               "    val localVal : Int = \"IntelliJ\" // Error, incompatible types\n" +
               "    println(anotherString + field + localVar + globalConst)\n" +
               "    val time = Date.parse(\"1.2.3\") // Method is deprecated\n" +
               "    var reassignedValue = \"\" as Int\n" +
               "    reassignedValue++\n" +
               "    object : Runnable {\n" +
               "      override fun() {\n" +
               "        val a = localVar\n" +
               "      }\n" +
               "    }\n" +
               "    reassignedParam = Array<Int>(2)\n" +
               "    reassignedParam[0] = 1\n" +
               "    reassignedParam.foreach @lit {\n" +
               "      if (it == 0) return@lit\n" +
               "      println(it + localVar)\n" +
               "    }\n" +
               "  }\n" +
               "}\n" +
               "trait ATrait {\n" +
               "  fun memberFun(param : (Int) -> Int)\n" +
               "}\n" +
               "abstract class SomeAbstractClass {\n" +
               "}";
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Collections.emptyMap();
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return new AttributesDescriptor[]{
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.keyword"), JetHighlightingColors.KEYWORD),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.builtin.annotation"), JetHighlightingColors.BUILTIN_ANNOTATION),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.number"), JetHighlightingColors.NUMBER),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.string"), JetHighlightingColors.STRING),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.valid.escape.in.string"), JetHighlightingColors.VALID_STRING_ESCAPE),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.invalid.escape.in.string"), JetHighlightingColors.INVALID_STRING_ESCAPE),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.operator.sign"), JetHighlightingColors.OPERATOR_SIGN),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.parentheses"), JetHighlightingColors.PARENTHESIS),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.braces"), JetHighlightingColors.BRACES),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.brackets"), JetHighlightingColors.BRACKETS),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.closure.braces"), JetHighlightingColors.FUNCTION_LITERAL_BRACES),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.comma"), JetHighlightingColors.COMMA),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.semicolon"), JetHighlightingColors.SEMICOLON),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.dot"), JetHighlightingColors.DOT),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.safe.access"), JetHighlightingColors.SAFE_ACCESS),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.arrow"), JetHighlightingColors.ARROW),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.line.comment"), JetHighlightingColors.LINE_COMMENT),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.block.comment"), JetHighlightingColors.BLOCK_COMMENT),

            // KDoc highlighting options are temporarily disabled, until actual highlighting and parsing of them is implemented
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.kdoc.comment"), JetHighlightingColors.DOC_COMMENT),
            //new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.kdoc.tag"), JetHighlightingColors.DOC_COMMENT_TAG),
            //new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.kdoc.tag.value"), JetHighlightingColors.DOC_COMMENT_TAG_VALUE),
            //new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.kdoc.markup"), JetHighlightingColors.DOC_COMMENT_MARKUP),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.class"), JetHighlightingColors.CLASS),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.type.parameter"), JetHighlightingColors.TYPE_PARAMETER),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.abstract.class"), JetHighlightingColors.ABSTRACT_CLASS),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.trait"), JetHighlightingColors.TRAIT),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.annotation"), JetHighlightingColors.ANNOTATION),

            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.var"), JetHighlightingColors.MUTABLE_VARIABLE),

            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.local.variable"), JetHighlightingColors.LOCAL_VARIABLE),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.parameter"), JetHighlightingColors.PARAMETER),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.bound.variable"), JetHighlightingColors.WRAPPED_INTO_REF),

            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.instance.property"), JetHighlightingColors.INSTANCE_PROPERTY),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.namespace.property"), JetHighlightingColors.NAMESPACE_PROPERTY),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.property.with.backing"), JetHighlightingColors.PROPERTY_WITH_BACKING_FIELD),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.backing.field.access"), JetHighlightingColors.BACKING_FIELD_ACCESS),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.extension.property"), JetHighlightingColors.EXTENSION_PROPERTY),

            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.it"), JetHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.fun"), JetHighlightingColors.FUNCTION_DECLARATION),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.fun.call"), JetHighlightingColors.FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.namespace.fun.call"), JetHighlightingColors.NAMESPACE_FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.extension.fun.call"), JetHighlightingColors.EXTENSION_FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.constructor.call"), JetHighlightingColors.CONSTRUCTOR_CALL),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.bad.character"), JetHighlightingColors.BAD_CHARACTER),

            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.auto.casted"), JetHighlightingColors.AUTO_CASTED_VALUE),

            new AttributesDescriptor(JetBundle.message("options.jet.attribute.descriptor.label"), JetHighlightingColors.LABEL),
        };
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Kotlin";
    }
}
