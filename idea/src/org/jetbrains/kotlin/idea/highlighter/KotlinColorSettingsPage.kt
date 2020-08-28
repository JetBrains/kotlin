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
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.highlighter.dsl.DslHighlighterExtension
import java.lang.reflect.Modifier
import java.util.*

class KotlinColorSettingsPage : ColorSettingsPage, RainbowColorSettingsPage {
    override fun getLanguage() = KotlinLanguage.INSTANCE
    override fun getIcon() = KotlinIcons.SMALL_LOGO
    override fun getHighlighter(): SyntaxHighlighter = KotlinHighlighter()

    override fun getDemoText(): String {
        return """/* Block comment */
<KEYWORD>package</KEYWORD> hello
<KEYWORD>import</KEYWORD> kotlin.collections.* // line comment

/**
 * Doc comment here for `SomeClass`
 * @see <KDOC_LINK>Iterator#next()</KDOC_LINK>
 */
<ANNOTATION>@Deprecated</ANNOTATION>(<ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES>message</ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES> = "Deprecated class")
<BUILTIN_ANNOTATION>private</BUILTIN_ANNOTATION> class <CLASS>MyClass</CLASS><<BUILTIN_ANNOTATION>out</BUILTIN_ANNOTATION> <TYPE_PARAMETER>T</TYPE_PARAMETER> : <TRAIT>Iterable</TRAIT><<TYPE_PARAMETER>T</TYPE_PARAMETER>>>(var <PARAMETER><MUTABLE_VARIABLE><INSTANCE_PROPERTY>prop1</INSTANCE_PROPERTY></MUTABLE_VARIABLE></PARAMETER> : Int) {
    fun <FUNCTION_DECLARATION>foo</FUNCTION_DECLARATION>(<PARAMETER>nullable</PARAMETER> : String<QUEST>?</QUEST>, <PARAMETER>r</PARAMETER> : <TRAIT>Runnable</TRAIT>, <PARAMETER>f</PARAMETER> : () -> Int, <PARAMETER>fl</PARAMETER> : <TRAIT>FunctionLike</TRAIT>, dyn: <KEYWORD>dynamic</KEYWORD>) {
        <PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL>("length\nis ${"$"}{<PARAMETER>nullable</PARAMETER><SAFE_ACCESS>?.</SAFE_ACCESS><INSTANCE_PROPERTY>length</INSTANCE_PROPERTY>} <STRING_ESCAPE><INVALID_STRING_ESCAPE>\e</INVALID_STRING_ESCAPE></STRING_ESCAPE>")
        <PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL>(<PARAMETER>nullable</PARAMETER><EXCLEXCL>!!</EXCLEXCL>.<INSTANCE_PROPERTY>length</INSTANCE_PROPERTY>)
        val <LOCAL_VARIABLE>ints</LOCAL_VARIABLE> = java.util.<CONSTRUCTOR_CALL>ArrayList</CONSTRUCTOR_CALL><Int?>(2)
        <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>[0] = 102 + <PARAMETER><VARIABLE_AS_FUNCTION_CALL>f</VARIABLE_AS_FUNCTION_CALL></PARAMETER>() + <PARAMETER><VARIABLE_AS_FUNCTION_LIKE_CALL>fl</VARIABLE_AS_FUNCTION_LIKE_CALL></PARAMETER>()
        val <LOCAL_VARIABLE>myFun</LOCAL_VARIABLE> = <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW> <FUNCTION_LITERAL_BRACES_AND_ARROW>-></FUNCTION_LITERAL_BRACES_AND_ARROW> "" <FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>;
        var <LOCAL_VARIABLE><MUTABLE_VARIABLE>ref</MUTABLE_VARIABLE></LOCAL_VARIABLE> = <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<INSTANCE_PROPERTY>size</INSTANCE_PROPERTY>
        ints.<EXTENSION_PROPERTY>lastIndex</EXTENSION_PROPERTY> + <PACKAGE_PROPERTY>globalCounter</PACKAGE_PROPERTY>
        <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<EXTENSION_FUNCTION_CALL>forEach</EXTENSION_FUNCTION_CALL> <LABEL>lit@</LABEL> <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW>
            <KEYWORD>if</KEYWORD> (<FUNCTION_LITERAL_DEFAULT_PARAMETER>it</FUNCTION_LITERAL_DEFAULT_PARAMETER> == null) return<LABEL>@lit</LABEL>
            <PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL>(<FUNCTION_LITERAL_DEFAULT_PARAMETER><SMART_CAST_VALUE>it</SMART_CAST_VALUE></FUNCTION_LITERAL_DEFAULT_PARAMETER> + <LOCAL_VARIABLE><MUTABLE_VARIABLE><WRAPPED_INTO_REF>ref</WRAPPED_INTO_REF></MUTABLE_VARIABLE></LOCAL_VARIABLE>)
        <FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>
        dyn.<DYNAMIC_FUNCTION_CALL>dynamicCall</DYNAMIC_FUNCTION_CALL>()
        dyn.<DYNAMIC_PROPERTY_CALL>dynamicProp</DYNAMIC_PROPERTY_CALL> = 5
        val <LOCAL_VARIABLE>klass</LOCAL_VARIABLE> = <CLASS>MyClass</CLASS>::<KEYWORD>class</KEYWORD>
        val year = java.time.LocalDate.now().<SYNTHETIC_EXTENSION_PROPERTY>year</SYNTHETIC_EXTENSION_PROPERTY>
    }

    <BUILTIN_ANNOTATION>override</BUILTIN_ANNOTATION> fun hashCode(): Int {
        return <KEYWORD>super</KEYWORD>.<FUNCTION_CALL>hashCode</FUNCTION_CALL>() * 31
    }
}

fun Int?.bar() {
    <KEYWORD>if</KEYWORD> (this != null) {
        println(<NAMED_ARGUMENT>message =</NAMED_ARGUMENT> <SMART_CAST_RECEIVER>toString</SMART_CAST_RECEIVER>())
    }
    else {
        println(<SMART_CONSTANT>this</SMART_CONSTANT>.toString())
    }
}

var <PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION><MUTABLE_VARIABLE>globalCounter</MUTABLE_VARIABLE></PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION> : Int = <NUMBER>5</NUMBER>
    <KEYWORD>get</KEYWORD>() = <LOCAL_VARIABLE><MUTABLE_VARIABLE><BACKING_FIELD_VARIABLE>field</BACKING_FIELD_VARIABLE></MUTABLE_VARIABLE></LOCAL_VARIABLE>

<KEYWORD>abstract</KEYWORD> class <ABSTRACT_CLASS>Abstract</ABSTRACT_CLASS> {
    val <INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>bar</INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION> <KEYWORD>get</KEYWORD>() = 1
    fun <FUNCTION_DECLARATION>test</FUNCTION_DECLARATION>() {
        <INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>bar</INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>
    }
}

<KEYWORD>object</KEYWORD> <OBJECT>Obj</OBJECT>

<KEYWORD>enum</KEYWORD> <KEYWORD>class</KEYWORD> <ENUM>E</ENUM> { <ENUM_ENTRY>A</ENUM_ENTRY>, <ENUM_ENTRY>B</ENUM_ENTRY> }

<KEYWORD>interface</KEYWORD> <TRAIT>FunctionLike</TRAIT> {
    <BUILTIN_ANNOTATION>operator</BUILTIN_ANNOTATION> <KEYWORD>fun</KEYWORD> <FUNCTION_DECLARATION>invoke</FUNCTION_DECLARATION>() = <NUMBER>1</NUMBER>
}

<KEYWORD>typealias</KEYWORD> <TYPE_ALIAS>Predicate</TYPE_ALIAS><<TYPE_PARAMETER>T</TYPE_PARAMETER>> = (<TYPE_PARAMETER>T</TYPE_PARAMETER>) -> <CLASS>Boolean</CLASS>
<KEYWORD>fun</KEYWORD> <FUNCTION_DECLARATION>baz</FUNCTION_DECLARATION>(<PARAMETER>p</PARAMETER>: <TYPE_ALIAS>Predicate</TYPE_ALIAS><<CLASS>Int</CLASS>>) = <PARAMETER><VARIABLE_AS_FUNCTION_CALL>p</VARIABLE_AS_FUNCTION_CALL></PARAMETER>(<NUMBER>42</NUMBER>)

<KEYWORD>suspend</KEYWORD> <KEYWORD>fun</KEYWORD> <FUNCTION_DECLARATION>suspendCall</FUNCTION_DECLARATION>() = 
  <SUSPEND_FUNCTION_CALL>suspendFn</SUSPEND_FUNCTION_CALL>()

<KEYWORD>suspend</KEYWORD> <KEYWORD>fun</KEYWORD> <FUNCTION_DECLARATION>suspendFn</FUNCTION_DECLARATION>() {}

"""
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        val map = HashMap<String, TextAttributesKey>()
        for (field in KotlinHighlightingColors::class.java.fields) {
            if (Modifier.isStatic(field.modifiers)) {
                try {
                    map.put(field.name, field.get(null) as TextAttributesKey)
                } catch (e: IllegalAccessException) {
                    assert(false)
                }

            }
        }

        map.putAll(DslHighlighterExtension.descriptionsToStyles)

        return map
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        infix fun String.to(key: TextAttributesKey) = AttributesDescriptor(this, key)

        return arrayOf(
            KotlinBundle.message("highlighter.descriptor.text.builtin.keyword") to KotlinHighlightingColors.KEYWORD,
            KotlinBundle.message("highlighter.descriptor.text.builtin.keyword.val") to KotlinHighlightingColors.VAL_KEYWORD,
            KotlinBundle.message("highlighter.descriptor.text.builtin.keyword.var") to KotlinHighlightingColors.VAR_KEYWORD,
            KotlinBundle.message("highlighter.descriptor.text.builtin.annotation") to KotlinHighlightingColors.BUILTIN_ANNOTATION,
            OptionsBundle.message("options.java.attribute.descriptor.number") to KotlinHighlightingColors.NUMBER,
            OptionsBundle.message("options.java.attribute.descriptor.string") to KotlinHighlightingColors.STRING,
            KotlinBundle.message("highlighter.descriptor.text.string.escape") to KotlinHighlightingColors.STRING_ESCAPE,
            OptionsBundle.message("options.java.attribute.descriptor.invalid.escape.in.string") to KotlinHighlightingColors.INVALID_STRING_ESCAPE,
            OptionsBundle.message("options.java.attribute.descriptor.operator.sign") to KotlinHighlightingColors.OPERATOR_SIGN,
            OptionsBundle.message("options.java.attribute.descriptor.parentheses") to KotlinHighlightingColors.PARENTHESIS,
            OptionsBundle.message("options.java.attribute.descriptor.braces") to KotlinHighlightingColors.BRACES,
            KotlinBundle.message("highlighter.descriptor.text.closure.braces") to KotlinHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW,
            KotlinBundle.message("highlighter.descriptor.text.arrow") to KotlinHighlightingColors.ARROW,
            OptionsBundle.message("options.java.attribute.descriptor.brackets") to KotlinHighlightingColors.BRACKETS,
            OptionsBundle.message("options.java.attribute.descriptor.comma") to KotlinHighlightingColors.COMMA,
            OptionsBundle.message("options.java.attribute.descriptor.semicolon") to KotlinHighlightingColors.SEMICOLON,
            KotlinBundle.message("highlighter.descriptor.text.colon") to KotlinHighlightingColors.COLON,
            KotlinBundle.message("highlighter.descriptor.text.double.colon") to KotlinHighlightingColors.DOUBLE_COLON,
            OptionsBundle.message("options.java.attribute.descriptor.dot") to KotlinHighlightingColors.DOT,
            KotlinBundle.message("highlighter.descriptor.text.safe.access") to KotlinHighlightingColors.SAFE_ACCESS,
            KotlinBundle.message("highlighter.descriptor.text.quest") to KotlinHighlightingColors.QUEST,
            KotlinBundle.message("highlighter.descriptor.text.exclexcl") to KotlinHighlightingColors.EXCLEXCL,
            OptionsBundle.message("options.java.attribute.descriptor.line.comment") to KotlinHighlightingColors.LINE_COMMENT,
            OptionsBundle.message("options.java.attribute.descriptor.block.comment") to KotlinHighlightingColors.BLOCK_COMMENT,
            KotlinBundle.message("highlighter.descriptor.text.kdoc.comment") to KotlinHighlightingColors.DOC_COMMENT,
            KotlinBundle.message("highlighter.descriptor.text.kdoc.tag") to KotlinHighlightingColors.KDOC_TAG,
            KotlinBundle.message("highlighter.descriptor.text.kdoc.value") to KotlinHighlightingColors.KDOC_LINK,
            OptionsBundle.message("options.java.attribute.descriptor.class") to KotlinHighlightingColors.CLASS,
            OptionsBundle.message("options.java.attribute.descriptor.type.parameter") to KotlinHighlightingColors.TYPE_PARAMETER,
            OptionsBundle.message("options.java.attribute.descriptor.abstract.class") to KotlinHighlightingColors.ABSTRACT_CLASS,
            OptionsBundle.message("options.java.attribute.descriptor.interface") to KotlinHighlightingColors.TRAIT,
            KotlinBundle.message("highlighter.descriptor.text.annotation") to KotlinHighlightingColors.ANNOTATION,
            KotlinBundle.message("highlighter.descriptor.text.annotation.attribute.name") to KotlinHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES,
            KotlinBundle.message("highlighter.descriptor.text.object") to KotlinHighlightingColors.OBJECT,
            KotlinBundle.message("highlighter.descriptor.text.enum") to KotlinHighlightingColors.ENUM,
            KotlinBundle.message("highlighter.descriptor.text.enumEntry") to KotlinHighlightingColors.ENUM_ENTRY,
            KotlinBundle.message("highlighter.descriptor.text.typeAlias") to KotlinHighlightingColors.TYPE_ALIAS,
            KotlinBundle.message("highlighter.descriptor.text.var") to KotlinHighlightingColors.MUTABLE_VARIABLE,
            KotlinBundle.message("highlighter.descriptor.text.local.variable") to KotlinHighlightingColors.LOCAL_VARIABLE,
            OptionsBundle.message("options.java.attribute.descriptor.parameter") to KotlinHighlightingColors.PARAMETER,
            KotlinBundle.message("highlighter.descriptor.text.captured.variable") to KotlinHighlightingColors.WRAPPED_INTO_REF,
            KotlinBundle.message("highlighter.descriptor.text.instance.property") to KotlinHighlightingColors.INSTANCE_PROPERTY,
            KotlinBundle.message("highlighter.descriptor.text.instance.property.custom.property.declaration") to KotlinHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
            KotlinBundle.message("highlighter.descriptor.text.package.property.custom.property.declaration") to KotlinHighlightingColors.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
            KotlinBundle.message("highlighter.descriptor.text.package.property") to KotlinHighlightingColors.PACKAGE_PROPERTY,
            KotlinBundle.message("highlighter.descriptor.text.field") to KotlinHighlightingColors.BACKING_FIELD_VARIABLE,
            KotlinBundle.message("highlighter.descriptor.text.extension.property") to KotlinHighlightingColors.EXTENSION_PROPERTY,
            KotlinBundle.message("highlighter.descriptor.text.synthetic.extension.property") to KotlinHighlightingColors.SYNTHETIC_EXTENSION_PROPERTY,
            KotlinBundle.message("highlighter.descriptor.text.dynamic.property") to KotlinHighlightingColors.DYNAMIC_PROPERTY_CALL,
            KotlinBundle.message("highlighter.descriptor.text.android.extensions.property") to KotlinHighlightingColors.ANDROID_EXTENSIONS_PROPERTY_CALL,
            KotlinBundle.message("highlighter.descriptor.text.it") to KotlinHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER,
            KotlinBundle.message("highlighter.descriptor.text.fun") to KotlinHighlightingColors.FUNCTION_DECLARATION,
            KotlinBundle.message("highlighter.descriptor.text.fun.call") to KotlinHighlightingColors.FUNCTION_CALL,
            KotlinBundle.message("highlighter.descriptor.text.dynamic.fun.call") to KotlinHighlightingColors.DYNAMIC_FUNCTION_CALL,
            KotlinBundle.message("highlighter.descriptor.text.suspend.fun.call") to KotlinHighlightingColors.SUSPEND_FUNCTION_CALL,
            KotlinBundle.message("highlighter.descriptor.text.package.fun.call") to KotlinHighlightingColors.PACKAGE_FUNCTION_CALL,
            KotlinBundle.message("highlighter.descriptor.text.extension.fun.call") to KotlinHighlightingColors.EXTENSION_FUNCTION_CALL,
            KotlinBundle.message("highlighter.descriptor.text.constructor.call") to KotlinHighlightingColors.CONSTRUCTOR_CALL,
            KotlinBundle.message("highlighter.descriptor.text.variable.as.function.call") to KotlinHighlightingColors.VARIABLE_AS_FUNCTION_CALL,
            KotlinBundle.message("highlighter.descriptor.text.variable.as.function.like.call") to KotlinHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL,
            KotlinBundle.message("highlighter.descriptor.text.smart.cast") to KotlinHighlightingColors.SMART_CAST_VALUE,
            KotlinBundle.message("highlighter.descriptor.text.smart.constant") to KotlinHighlightingColors.SMART_CONSTANT,
            KotlinBundle.message("highlighter.descriptor.text.smart.cast.receiver") to KotlinHighlightingColors.SMART_CAST_RECEIVER,
            KotlinBundle.message("highlighter.descriptor.text.label") to KotlinHighlightingColors.LABEL,
            KotlinBundle.message("highlighter.descriptor.text.named.argument") to KotlinHighlightingColors.NAMED_ARGUMENT
        ) + DslHighlighterExtension.descriptionsToStyles.map { (description, key) -> description to key }.toTypedArray()
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = KotlinLanguage.NAME

    override fun isRainbowType(type: TextAttributesKey): Boolean {
        return type == KotlinHighlightingColors.LOCAL_VARIABLE ||
                type == KotlinHighlightingColors.PARAMETER
    }
}
