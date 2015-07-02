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
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.JetLanguage

import javax.swing.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.HashMap

public class KotlinColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = JetIcons.SMALL_LOGO
    override fun getHighlighter(): SyntaxHighlighter = JetHighlighter()

    override fun getDemoText(): String {
        return """/* Block comment */
<KEYWORD>package</KEYWORD> hello
<KEYWORD>import</KEYWORD> kotlin.util.* // line comment

/**
 * Doc comment here for `SomeClass`
 * @see <KDOC_LINK>Iterator#next()</KDOC_LINK>
 */
<ANNOTATION>@deprecated</ANNOTATION>("Deprecated class")
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
        return <BACKING_FIELD_ACCESS><PACKAGE_PROPERTY><MUTABLE_VARIABLE>${"$"}globalCounter</MUTABLE_VARIABLE></PACKAGE_PROPERTY></BACKING_FIELD_ACCESS>
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
        for (field in javaClass<JetHighlightingColors>().getFields()) {
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
        fun String.to(key: TextAttributesKey) = AttributesDescriptor(this, key)
        
        return arrayOf(OptionsBundle.message("options.java.attribute.descriptor.keyword") to JetHighlightingColors.KEYWORD, 
                       JetBundle.message("options.kotlin.attribute.descriptor.builtin.annotation") to JetHighlightingColors.BUILTIN_ANNOTATION, 
                       OptionsBundle.message("options.java.attribute.descriptor.number") to JetHighlightingColors.NUMBER,
                       OptionsBundle.message("options.java.attribute.descriptor.string") to JetHighlightingColors.STRING, 
                       JetBundle.message("options.kotlin.attribute.descriptor.string.escape") to JetHighlightingColors.STRING_ESCAPE, 
                       OptionsBundle.message("options.java.attribute.descriptor.invalid.escape.in.string") to JetHighlightingColors.INVALID_STRING_ESCAPE,
                       OptionsBundle.message("options.java.attribute.descriptor.operator.sign") to JetHighlightingColors.OPERATOR_SIGN, 
                       OptionsBundle.message("options.java.attribute.descriptor.parentheses") to JetHighlightingColors.PARENTHESIS, 
                       OptionsBundle.message("options.java.attribute.descriptor.braces") to JetHighlightingColors.BRACES, 
                       JetBundle.message("options.kotlin.attribute.descriptor.closure.braces") to JetHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW, 
                       JetBundle.message("options.kotlin.attribute.descriptor.arrow") to JetHighlightingColors.ARROW, 
                       OptionsBundle.message("options.java.attribute.descriptor.brackets") to JetHighlightingColors.BRACKETS, 
                       OptionsBundle.message("options.java.attribute.descriptor.comma") to JetHighlightingColors.COMMA, 
                       OptionsBundle.message("options.java.attribute.descriptor.semicolon") to JetHighlightingColors.SEMICOLON, 
                       OptionsBundle.message("options.java.attribute.descriptor.dot") to JetHighlightingColors.DOT, 
                       JetBundle.message("options.kotlin.attribute.descriptor.safe.access") to JetHighlightingColors.SAFE_ACCESS,
                       OptionsBundle.message("options.java.attribute.descriptor.line.comment") to JetHighlightingColors.LINE_COMMENT, 
                       OptionsBundle.message("options.java.attribute.descriptor.block.comment") to JetHighlightingColors.BLOCK_COMMENT,
                       JetBundle.message("options.kotlin.attribute.descriptor.kdoc.comment") to JetHighlightingColors.DOC_COMMENT, 
                       JetBundle.message("options.kotlin.attribute.descriptor.kdoc.tag") to JetHighlightingColors.KDOC_TAG, 
                       JetBundle.message("options.kotlin.attribute.descriptor.kdoc.value") to JetHighlightingColors.KDOC_LINK,
                       OptionsBundle.message("options.java.attribute.descriptor.class") to JetHighlightingColors.CLASS, 
                       OptionsBundle.message("options.java.attribute.descriptor.type.parameter") to JetHighlightingColors.TYPE_PARAMETER, 
                       OptionsBundle.message("options.java.attribute.descriptor.abstract.class") to JetHighlightingColors.ABSTRACT_CLASS, 
                       "Interface" to JetHighlightingColors.TRAIT, 
                       JetBundle.message("options.kotlin.attribute.descriptor.annotation") to JetHighlightingColors.ANNOTATION, 
                       JetBundle.message("options.kotlin.attribute.descriptor.object") to JetHighlightingColors.OBJECT, 
                       JetBundle.message("options.kotlin.attribute.descriptor.enumEntry") to JetHighlightingColors.ENUM_ENTRY,
                       JetBundle.message("options.kotlin.attribute.descriptor.var") to JetHighlightingColors.MUTABLE_VARIABLE,
                       JetBundle.message("options.kotlin.attribute.descriptor.local.variable") to JetHighlightingColors.LOCAL_VARIABLE, 
                       OptionsBundle.message("options.java.attribute.descriptor.parameter") to JetHighlightingColors.PARAMETER, 
                       JetBundle.message("options.kotlin.attribute.descriptor.captured.variable") to JetHighlightingColors.WRAPPED_INTO_REF,
                       JetBundle.message("options.kotlin.attribute.descriptor.instance.property") to JetHighlightingColors.INSTANCE_PROPERTY, 
                       JetBundle.message("options.kotlin.attribute.descriptor.package.property") to JetHighlightingColors.PACKAGE_PROPERTY, 
                       JetBundle.message("options.kotlin.attribute.descriptor.property.with.backing") to JetHighlightingColors.PROPERTY_WITH_BACKING_FIELD,
                       JetBundle.message("options.kotlin.attribute.descriptor.backing.field.access") to JetHighlightingColors.BACKING_FIELD_ACCESS,
                       JetBundle.message("options.kotlin.attribute.descriptor.extension.property") to JetHighlightingColors.EXTENSION_PROPERTY,
                       JetBundle.message("options.kotlin.attribute.descriptor.dynamic.property") to JetHighlightingColors.DYNAMIC_PROPERTY_CALL,
                       JetBundle.message("options.kotlin.attribute.descriptor.it") to JetHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER,
                       JetBundle.message("options.kotlin.attribute.descriptor.fun") to JetHighlightingColors.FUNCTION_DECLARATION, 
                       JetBundle.message("options.kotlin.attribute.descriptor.fun.call") to JetHighlightingColors.FUNCTION_CALL, 
                       JetBundle.message("options.kotlin.attribute.descriptor.dynamic.fun.call") to JetHighlightingColors.DYNAMIC_FUNCTION_CALL, 
                       JetBundle.message("options.kotlin.attribute.descriptor.package.fun.call") to JetHighlightingColors.PACKAGE_FUNCTION_CALL, 
                       JetBundle.message("options.kotlin.attribute.descriptor.extension.fun.call") to JetHighlightingColors.EXTENSION_FUNCTION_CALL, 
                       JetBundle.message("options.kotlin.attribute.descriptor.constructor.call") to JetHighlightingColors.CONSTRUCTOR_CALL, 
                       JetBundle.message("options.kotlin.attribute.descriptor.variable.as.function.call") to JetHighlightingColors.VARIABLE_AS_FUNCTION_CALL, 
                       JetBundle.message("options.kotlin.attribute.descriptor.variable.as.function.like.call") to JetHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL,
                       OptionsBundle.message("options.java.attribute.descriptor.bad.character") to JetHighlightingColors.BAD_CHARACTER,
                       JetBundle.message("options.kotlin.attribute.descriptor.smart.cast") to JetHighlightingColors.SMART_CAST_VALUE,
                       JetBundle.message("options.kotlin.attribute.descriptor.label") to JetHighlightingColors.LABEL)
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = JetLanguage.NAME
}
