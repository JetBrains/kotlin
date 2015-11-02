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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.OptionsBundle
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinLanguage

import javax.swing.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.HashMap

public class KotlinColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = KotlinIcons.SMALL_LOGO
    override fun getHighlighter(): SyntaxHighlighter = KotlinHighlighter()

    override fun getDemoText(): String {
        return """/* Block comment */
<KEYWORD>package</KEYWORD> hello
<KEYWORD>import</KEYWORD> kotlin.util.* // line comment

/**
 * Doc comment here for `SomeClass`
 * @see <KDOC_LINK>Iterator#next()</KDOC_LINK>
 */
<ANNOTATION>@Deprecated</ANNOTATION>("Deprecated class")
<BUILTIN_ANNOTATION>public</BUILTIN_ANNOTATION> class <CLASS>MyClass</CLASS><<BUILTIN_ANNOTATION>out</BUILTIN_ANNOTATION> <TYPE_PARAMETER>T</TYPE_PARAMETER> : <TRAIT>Iterable</TRAIT><<TYPE_PARAMETER>T</TYPE_PARAMETER>>>(var <INSTANCE_PROPERTY><MUTABLE_VARIABLE>prop1</MUTABLE_VARIABLE></INSTANCE_PROPERTY> : Int)
    fun <FUNCTION_DECLARATION>foo</FUNCTION_DECLARATION>(<PARAMETER>nullable</PARAMETER> : String?, <PARAMETER>r</PARAMETER> : <TRAIT>Runnable</TRAIT>, <PARAMETER>f</PARAMETER> : () -> Int, <PARAMETER>fl</PARAMETER> : <TRAIT>FunctionLike</TRAIT>, dyn: dynamic) {
        <FUNCTION_CALL><PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL></FUNCTION_CALL>("length\nis ${"$"}{<PARAMETER>nullable</PARAMETER><SAFE_ACCESS>?.</SAFE_ACCESS><INSTANCE_PROPERTY>length</INSTANCE_PROPERTY>} <INVALID_STRING_ESCAPE><STRING_ESCAPE>\e</STRING_ESCAPE></INVALID_STRING_ESCAPE>")
        val <LOCAL_VARIABLE>ints</LOCAL_VARIABLE> = java.util.<CONSTRUCTOR_CALL>ArrayList</CONSTRUCTOR_CALL><Int?>(2)
        <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>[0] = 102 + <PARAMETER><VARIABLE_AS_FUNCTION_CALL>f</VARIABLE_AS_FUNCTION_CALL></PARAMETER>() + <PARAMETER><VARIABLE_AS_FUNCTION_LIKE_CALL>fl</VARIABLE_AS_FUNCTION_LIKE_CALL></PARAMETER>()
        val <LOCAL_VARIABLE>myFun</LOCAL_VARIABLE> = <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW> <FUNCTION_LITERAL_BRACES_AND_ARROW>-></FUNCTION_LITERAL_BRACES_AND_ARROW> "" <FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>;
        var <LOCAL_VARIABLE><MUTABLE_VARIABLE><WRAPPED_INTO_REF>ref</WRAPPED_INTO_REF></MUTABLE_VARIABLE></LOCAL_VARIABLE> = <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<FUNCTION_CALL>size</FUNCTION_CALL>()
        if (!<LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<EXTENSION_PROPERTY><PACKAGE_PROPERTY>empty</PACKAGE_PROPERTY></EXTENSION_PROPERTY>) {
              <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<EXTENSION_FUNCTION_CALL><PACKAGE_FUNCTION_CALL><FUNCTION_CALL>forEach</FUNCTION_CALL></PACKAGE_FUNCTION_CALL></EXTENSION_FUNCTION_CALL> <LABEL>lit@</LABEL> <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW>
                  if (<FUNCTION_LITERAL_DEFAULT_PARAMETER>it</FUNCTION_LITERAL_DEFAULT_PARAMETER> == null) return<LABEL>@lit</LABEL>
                  <FUNCTION_CALL><PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL></FUNCTION_CALL>(<FUNCTION_LITERAL_DEFAULT_PARAMETER><SMART_CAST_VALUE>it</SMART_CAST_VALUE></FUNCTION_LITERAL_DEFAULT_PARAMETER> + <LOCAL_VARIABLE><MUTABLE_VARIABLE><WRAPPED_INTO_REF>ref</WRAPPED_INTO_REF></MUTABLE_VARIABLE></LOCAL_VARIABLE>)
              <FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>
        }
        dyn.<DYNAMIC_FUNCTION_CALL>dynamicCall</DYNAMIC_FUNCTION_CALL>()
        dyn.<DYNAMIC_PROPERTY_CALL>dynamicProp</DYNAMIC_PROPERTY_CALL> = 5
    }
}

var <PROPERTY_WITH_BACKING_FIELD><PACKAGE_PROPERTY><MUTABLE_VARIABLE>globalCounter</MUTABLE_VARIABLE></PACKAGE_PROPERTY></PROPERTY_WITH_BACKING_FIELD> : Int = 5
    <KEYWORD>get</KEYWORD>() {
        return <BACKING_FIELD_VARIABLE><LOCAL_VARIABLE><MUTABLE_VARIABLE>field</MUTABLE_VARIABLE></LOCAL_VARIABLE></BACKING_FIELD_VARIABLE>
    }

<KEYWORD>public</KEYWORD> <KEYWORD>abstract</KEYWORD> class <ABSTRACT_CLASS>Abstract</ABSTRACT_CLASS> {
}

<KEYWORD>object</KEYWORD> <OBJECT>Obj</OBJECT>

<KEYWORD>enum</KEYWORD> <KEYWORD>class</KEYWORD> <CLASS>E</CLASS> { <ENUM_ENTRY>A</ENUM_ENTRY> }
               Bad character: \n
"""
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        val map = HashMap<String, TextAttributesKey>()
        for (field in javaClass<KotlinHighlightingColors>().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    map.put(field.getName(), field.get(null) as TextAttributesKey)
                }
                catch (e: IllegalAccessException) {
                    assert(false)
                }

            }
        }
        return map
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        infix fun String.to(key: TextAttributesKey) = AttributesDescriptor(this, key)
        
        return arrayOf(OptionsBundle.message("options.java.attribute.descriptor.keyword") to KotlinHighlightingColors.KEYWORD,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.builtin.annotation") to KotlinHighlightingColors.BUILTIN_ANNOTATION,
                       OptionsBundle.message("options.java.attribute.descriptor.number") to KotlinHighlightingColors.NUMBER,
                       OptionsBundle.message("options.java.attribute.descriptor.string") to KotlinHighlightingColors.STRING,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.string.escape") to KotlinHighlightingColors.STRING_ESCAPE,
                       OptionsBundle.message("options.java.attribute.descriptor.invalid.escape.in.string") to KotlinHighlightingColors.INVALID_STRING_ESCAPE,
                       OptionsBundle.message("options.java.attribute.descriptor.operator.sign") to KotlinHighlightingColors.OPERATOR_SIGN,
                       OptionsBundle.message("options.java.attribute.descriptor.parentheses") to KotlinHighlightingColors.PARENTHESIS,
                       OptionsBundle.message("options.java.attribute.descriptor.braces") to KotlinHighlightingColors.BRACES,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.closure.braces") to KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.arrow") to KotlinHighlightingColors.ARROW,
                       OptionsBundle.message("options.java.attribute.descriptor.brackets") to KotlinHighlightingColors.BRACKETS,
                       OptionsBundle.message("options.java.attribute.descriptor.comma") to KotlinHighlightingColors.COMMA,
                       OptionsBundle.message("options.java.attribute.descriptor.semicolon") to KotlinHighlightingColors.SEMICOLON,
                       OptionsBundle.message("options.java.attribute.descriptor.dot") to KotlinHighlightingColors.DOT,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.safe.access") to KotlinHighlightingColors.SAFE_ACCESS,
                       OptionsBundle.message("options.java.attribute.descriptor.line.comment") to KotlinHighlightingColors.LINE_COMMENT,
                       OptionsBundle.message("options.java.attribute.descriptor.block.comment") to KotlinHighlightingColors.BLOCK_COMMENT,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.kdoc.comment") to KotlinHighlightingColors.DOC_COMMENT,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.kdoc.tag") to KotlinHighlightingColors.KDOC_TAG,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.kdoc.value") to KotlinHighlightingColors.KDOC_LINK,
                       OptionsBundle.message("options.java.attribute.descriptor.class") to KotlinHighlightingColors.CLASS,
                       OptionsBundle.message("options.java.attribute.descriptor.type.parameter") to KotlinHighlightingColors.TYPE_PARAMETER,
                       OptionsBundle.message("options.java.attribute.descriptor.abstract.class") to KotlinHighlightingColors.ABSTRACT_CLASS,
                       "Interface" to KotlinHighlightingColors.TRAIT,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.annotation") to KotlinHighlightingColors.ANNOTATION,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.object") to KotlinHighlightingColors.OBJECT,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.enumEntry") to KotlinHighlightingColors.ENUM_ENTRY,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.var") to KotlinHighlightingColors.MUTABLE_VARIABLE,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.local.variable") to KotlinHighlightingColors.LOCAL_VARIABLE,
                       OptionsBundle.message("options.java.attribute.descriptor.parameter") to KotlinHighlightingColors.PARAMETER,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.captured.variable") to KotlinHighlightingColors.WRAPPED_INTO_REF,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.instance.property") to KotlinHighlightingColors.INSTANCE_PROPERTY,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.package.property") to KotlinHighlightingColors.PACKAGE_PROPERTY,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.property.with.backing") to KotlinHighlightingColors.PROPERTY_WITH_BACKING_FIELD,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.field") to KotlinHighlightingColors.BACKING_FIELD_VARIABLE,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.extension.property") to KotlinHighlightingColors.EXTENSION_PROPERTY,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.dynamic.property") to KotlinHighlightingColors.DYNAMIC_PROPERTY_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.it") to KotlinHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.fun") to KotlinHighlightingColors.FUNCTION_DECLARATION,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.fun.call") to KotlinHighlightingColors.FUNCTION_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.dynamic.fun.call") to KotlinHighlightingColors.DYNAMIC_FUNCTION_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.package.fun.call") to KotlinHighlightingColors.PACKAGE_FUNCTION_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.extension.fun.call") to KotlinHighlightingColors.EXTENSION_FUNCTION_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.constructor.call") to KotlinHighlightingColors.CONSTRUCTOR_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.variable.as.function.call") to KotlinHighlightingColors.VARIABLE_AS_FUNCTION_CALL,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.variable.as.function.like.call") to KotlinHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL,
                       OptionsBundle.message("options.java.attribute.descriptor.bad.character") to KotlinHighlightingColors.BAD_CHARACTER,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.smart.cast") to KotlinHighlightingColors.SMART_CAST_VALUE,
                       KotlinBundle.message("options.kotlin.attribute.descriptor.label") to KotlinHighlightingColors.LABEL)
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = KotlinLanguage.NAME
}
