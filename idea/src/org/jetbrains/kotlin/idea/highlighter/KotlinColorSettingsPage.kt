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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.JetIcons;
import org.jetbrains.kotlin.idea.JetLanguage;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class JetColorSettingsPage implements ColorSettingsPage {
    @Override
    public Icon getIcon() {
        return JetIcons.SMALL_LOGO;
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
               "<KEYWORD>package</KEYWORD> hello\n" +
               "<KEYWORD>import</KEYWORD> kotlin.util.* // line comment\n" +
               "\n" +
               "/**\n" +
               " * Doc comment here for `SomeClass`\n" +
               " * @see Iterator#next()\n" +
               " */\n" +
               "<ANNOTATION>@deprecated</ANNOTATION>(\"Deprecated class\")\n" +
               "<BUILTIN_ANNOTATION>public</BUILTIN_ANNOTATION> class <CLASS>MyClass</CLASS><<BUILTIN_ANNOTATION>out</BUILTIN_ANNOTATION> <TYPE_PARAMETER>T</TYPE_PARAMETER> : <TRAIT>Iterable</TRAIT><<TYPE_PARAMETER>T</TYPE_PARAMETER>>>(var <INSTANCE_PROPERTY><MUTABLE_VARIABLE>prop1</MUTABLE_VARIABLE></INSTANCE_PROPERTY> : Int) {\n" +
               "    fun <FUNCTION_DECLARATION>foo</FUNCTION_DECLARATION>(<PARAMETER>nullable</PARAMETER> : String?, <PARAMETER>r</PARAMETER> : <TRAIT>Runnable</TRAIT>, <PARAMETER>f</PARAMETER> : () -> Int, <PARAMETER>fl</PARAMETER> : <TRAIT>FunctionLike</TRAIT>, dyn: dynamic) {\n" +
               "        <FUNCTION_CALL><PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL></FUNCTION_CALL>(\"length\\nis ${<PARAMETER>nullable</PARAMETER><SAFE_ACCESS>?.</SAFE_ACCESS><INSTANCE_PROPERTY>length</INSTANCE_PROPERTY>} <INVALID_STRING_ESCAPE><STRING_ESCAPE>\\e</STRING_ESCAPE></INVALID_STRING_ESCAPE>\")\n" +
               "        val <LOCAL_VARIABLE>ints</LOCAL_VARIABLE> = java.util.<CONSTRUCTOR_CALL>ArrayList</CONSTRUCTOR_CALL><Int?>(2)\n" +
               "        <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>[0] = 102 + <PARAMETER><VARIABLE_AS_FUNCTION_CALL>f</VARIABLE_AS_FUNCTION_CALL></PARAMETER>() + <PARAMETER><VARIABLE_AS_FUNCTION_LIKE_CALL>fl</VARIABLE_AS_FUNCTION_LIKE_CALL></PARAMETER>()\n" +
               "        val <LOCAL_VARIABLE>myFun</LOCAL_VARIABLE> = <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW> <FUNCTION_LITERAL_BRACES_AND_ARROW>-></FUNCTION_LITERAL_BRACES_AND_ARROW> \"\" <FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>;\n" +
               "        var <LOCAL_VARIABLE><MUTABLE_VARIABLE><WRAPPED_INTO_REF>ref</WRAPPED_INTO_REF></MUTABLE_VARIABLE></LOCAL_VARIABLE> = <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<FUNCTION_CALL>size</FUNCTION_CALL>()\n" +
               "        if (!<LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<EXTENSION_PROPERTY><PACKAGE_PROPERTY>empty</PACKAGE_PROPERTY></EXTENSION_PROPERTY>) {\n" +
               "            <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<EXTENSION_FUNCTION_CALL><PACKAGE_FUNCTION_CALL><FUNCTION_CALL>forEach</FUNCTION_CALL></PACKAGE_FUNCTION_CALL></EXTENSION_FUNCTION_CALL> <LABEL>lit@</LABEL> <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW>\n" +
               "                if (<FUNCTION_LITERAL_DEFAULT_PARAMETER>it</FUNCTION_LITERAL_DEFAULT_PARAMETER> == null) return<LABEL>@lit</LABEL>\n" +
               "                <FUNCTION_CALL><PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL></FUNCTION_CALL>(<FUNCTION_LITERAL_DEFAULT_PARAMETER><SMART_CAST_VALUE>it</SMART_CAST_VALUE></FUNCTION_LITERAL_DEFAULT_PARAMETER> + <LOCAL_VARIABLE><MUTABLE_VARIABLE><WRAPPED_INTO_REF>ref</WRAPPED_INTO_REF></MUTABLE_VARIABLE></LOCAL_VARIABLE>)\n" +
               "            <FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>\n" +
               "        }\n" +
               "        dyn.<DYNAMIC_FUNCTION_CALL>dynamicCall</DYNAMIC_FUNCTION_CALL>()\n" +
               "        dyn.<DYNAMIC_PROPERTY_CALL>dynamicProp</DYNAMIC_PROPERTY_CALL> = 5\n" +
               "    }\n" +
               "}\n" +
               "\n" +
               "var <PROPERTY_WITH_BACKING_FIELD><PACKAGE_PROPERTY><MUTABLE_VARIABLE>globalCounter</MUTABLE_VARIABLE></PACKAGE_PROPERTY></PROPERTY_WITH_BACKING_FIELD> : Int = 5\n" +
               "    <KEYWORD>get</KEYWORD>() {\n" +
               "        return <BACKING_FIELD_ACCESS><PACKAGE_PROPERTY><MUTABLE_VARIABLE>$globalCounter</MUTABLE_VARIABLE></PACKAGE_PROPERTY></BACKING_FIELD_ACCESS>\n" +
               "    }\n" +
               "\n" +
               "<KEYWORD>public</KEYWORD> <KEYWORD>abstract</KEYWORD> class <ABSTRACT_CLASS>Abstract</ABSTRACT_CLASS> {\n" +
               "}\n" +
               "\n" +
               "<KEYWORD>object</KEYWORD> <OBJECT>Obj</OBJECT>\n" +
               "\n" +
               "<KEYWORD>enum</KEYWORD> <KEYWORD>class</KEYWORD> <CLASS>E</CLASS> { <ENUM_ENTRY>A</ENUM_ENTRY> }\n" +
               "               Bad character: \\n\n";
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        Map<String, TextAttributesKey> map = new HashMap<String, TextAttributesKey>();
        for (Field field : JetHighlightingColors.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    map.put(field.getName(), (TextAttributesKey) field.get(null));
                }
                catch (IllegalAccessException e) {
                    assert false;
                }
            }
        }
        return map;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return new AttributesDescriptor[]{
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.keyword"), JetHighlightingColors.KEYWORD),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.builtin.annotation"), JetHighlightingColors.BUILTIN_ANNOTATION),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.number"), JetHighlightingColors.NUMBER),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.string"), JetHighlightingColors.STRING),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.string.escape"), JetHighlightingColors.STRING_ESCAPE),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.invalid.escape.in.string"), JetHighlightingColors.INVALID_STRING_ESCAPE),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.operator.sign"), JetHighlightingColors.OPERATOR_SIGN),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.parentheses"), JetHighlightingColors.PARENTHESIS),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.braces"), JetHighlightingColors.BRACES),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.closure.braces"), JetHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.arrow"), JetHighlightingColors.ARROW),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.brackets"), JetHighlightingColors.BRACKETS),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.comma"), JetHighlightingColors.COMMA),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.semicolon"), JetHighlightingColors.SEMICOLON),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.dot"), JetHighlightingColors.DOT),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.safe.access"), JetHighlightingColors.SAFE_ACCESS),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.line.comment"), JetHighlightingColors.LINE_COMMENT),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.block.comment"), JetHighlightingColors.BLOCK_COMMENT),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.kdoc.comment"), JetHighlightingColors.DOC_COMMENT),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.kdoc.tag"), JetHighlightingColors.KDOC_TAG),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.kdoc.value"), JetHighlightingColors.KDOC_LINK),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.class"), JetHighlightingColors.CLASS),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.type.parameter"), JetHighlightingColors.TYPE_PARAMETER),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.abstract.class"), JetHighlightingColors.ABSTRACT_CLASS),
            new AttributesDescriptor("Interface", JetHighlightingColors.TRAIT),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.annotation"), JetHighlightingColors.ANNOTATION),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.object"), JetHighlightingColors.OBJECT),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.enumEntry"), JetHighlightingColors.ENUM_ENTRY),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.var"), JetHighlightingColors.MUTABLE_VARIABLE),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.local.variable"), JetHighlightingColors.LOCAL_VARIABLE),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.parameter"), JetHighlightingColors.PARAMETER),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.captured.variable"), JetHighlightingColors.WRAPPED_INTO_REF),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.instance.property"), JetHighlightingColors.INSTANCE_PROPERTY),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.package.property"), JetHighlightingColors.PACKAGE_PROPERTY),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.property.with.backing"), JetHighlightingColors.PROPERTY_WITH_BACKING_FIELD),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.backing.field.access"), JetHighlightingColors.BACKING_FIELD_ACCESS),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.extension.property"), JetHighlightingColors.EXTENSION_PROPERTY),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.dynamic.property"), JetHighlightingColors.DYNAMIC_PROPERTY_CALL),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.it"), JetHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.fun"), JetHighlightingColors.FUNCTION_DECLARATION),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.fun.call"), JetHighlightingColors.FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.dynamic.fun.call"), JetHighlightingColors.DYNAMIC_FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.package.fun.call"), JetHighlightingColors.PACKAGE_FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.extension.fun.call"), JetHighlightingColors.EXTENSION_FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.constructor.call"), JetHighlightingColors.CONSTRUCTOR_CALL),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.variable.as.function.call"), JetHighlightingColors.VARIABLE_AS_FUNCTION_CALL),
            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.variable.as.function.like.call"), JetHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL),

            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.bad.character"), JetHighlightingColors.BAD_CHARACTER),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.smart.cast"), JetHighlightingColors.SMART_CAST_VALUE),

            new AttributesDescriptor(JetBundle.message("options.kotlin.attribute.descriptor.label"), JetHighlightingColors.LABEL),
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
        return JetLanguage.NAME;
    }
}
