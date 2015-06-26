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

package org.jetbrains.kotlin.idea.core.refactoring

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.lexer.JetLexer
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker

import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

public object JetNameSuggester {

    private fun addName(result: ArrayList<String>, name: String?, validator: JetNameValidator) {
        var name = name ?: return
        if ("class" == name) name = "clazz"
        if (!isIdentifier(name)) return
        result.add(validator.validateName(name))
    }

    /**
     * Name suggestion types:
     * 1. According to type:
     * 1a. Primitive types to some short name
     * 1b. Class types according to class name camel humps: (AbCd => {abCd, cd})
     * 1c. Arrays => arrayOfInnerType
     * 2. Reference expressions according to reference name camel humps
     * 3. Method call expression according to method callee expression
     * @param expression to suggest name for variable
     * *
     * @param validator to check scope for such names
     * *
     * @param defaultName
     * *
     * @return possible names
     */
    public fun suggestNames(expression: JetExpression, validator: JetNameValidator, defaultName: String?): Array<String> {
        val result = ArrayList<String>()

        val bindingContext = expression.analyze(BodyResolveMode.FULL)
        val jetType = bindingContext.getType(expression)
        if (jetType != null) {
            addNamesForType(result, jetType, validator)
        }
        addNamesForExpression(result, expression, validator)

        if (result.isEmpty()) addName(result, defaultName, validator)
        return ArrayUtil.toStringArray(result)
    }

    public fun suggestNames(type: JetType, validator: JetNameValidator, defaultName: String?): Array<String> {
        val result = ArrayList<String>()
        addNamesForType(result, type, validator)
        if (result.isEmpty()) addName(result, defaultName, validator)
        return ArrayUtil.toStringArray(result)
    }

    public fun suggestNamesForType(jetType: JetType, validator: JetNameValidator): Array<String> {
        val result = ArrayList<String>()
        addNamesForType(result, jetType, validator)
        return ArrayUtil.toStringArray(result)
    }

    jvmOverloads public fun suggestNamesForExpression(expression: JetExpression, validator: JetNameValidator, defaultName: String? = null): Array<String> {
        val result = ArrayList<String>()
        addNamesForExpression(result, expression, validator)
        if (result.isEmpty()) addName(result, defaultName, validator)
        return ArrayUtil.toStringArray(result)
    }

    private val COMMON_TYPE_PARAMETER_NAMES = arrayOf("T", "U", "V", "W", "X", "Y", "Z")

    public fun suggestNamesForTypeParameters(count: Int, validator: JetNameValidator): Array<String> {
        val result = ArrayList<String>()
        for (i in 0..count - 1) {
            result.add(validator.validateNameWithVariants(*COMMON_TYPE_PARAMETER_NAMES))
        }
        return ArrayUtil.toStringArray(result)
    }

    private fun addNamesForType(result: ArrayList<String>, jetType: JetType, validator: JetNameValidator) {
        var jetType = jetType
        val builtIns = KotlinBuiltIns.getInstance()
        val typeChecker = JetTypeChecker.DEFAULT
        jetType = TypeUtils.makeNotNullable(jetType) // wipe out '?'
        if (ErrorUtils.containsErrorType(jetType)) return
        if (typeChecker.equalTypes(builtIns.getBooleanType(), jetType)) {
            addName(result, "b", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getIntType(), jetType)) {
            addName(result, "i", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getByteType(), jetType)) {
            addName(result, "byte", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getLongType(), jetType)) {
            addName(result, "l", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getFloatType(), jetType)) {
            addName(result, "fl", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getDoubleType(), jetType)) {
            addName(result, "d", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getShortType(), jetType)) {
            addName(result, "sh", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getCharType(), jetType)) {
            addName(result, "c", validator)
        }
        else if (typeChecker.equalTypes(builtIns.getStringType(), jetType)) {
            addName(result, "s", validator)
        }
        else if (KotlinBuiltIns.isArray(jetType) || KotlinBuiltIns.isPrimitiveArray(jetType)) {
            val elementType = KotlinBuiltIns.getInstance().getArrayElementType(jetType)
            if (typeChecker.equalTypes(builtIns.getBooleanType(), elementType)) {
                addName(result, "booleans", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getIntType(), elementType)) {
                addName(result, "ints", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getByteType(), elementType)) {
                addName(result, "bytes", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getLongType(), elementType)) {
                addName(result, "longs", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getFloatType(), elementType)) {
                addName(result, "floats", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getDoubleType(), elementType)) {
                addName(result, "doubles", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getShortType(), elementType)) {
                addName(result, "shorts", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getCharType(), elementType)) {
                addName(result, "chars", validator)
            }
            else if (typeChecker.equalTypes(builtIns.getStringType(), elementType)) {
                addName(result, "strings", validator)
            }
            else {
                val classDescriptor = TypeUtils.getClassDescriptor(elementType)
                if (classDescriptor != null) {
                    val className = classDescriptor.getName()
                    addName(result, "arrayOf" + StringUtil.capitalize(className.asString()) + "s", validator)
                }
            }
        }
        else {
            addForClassType(result, jetType, validator)
        }
    }

    private fun addForClassType(result: ArrayList<String>, jetType: JetType, validator: JetNameValidator) {
        val descriptor = jetType.getConstructor().getDeclarationDescriptor()
        if (descriptor != null) {
            val className = descriptor.getName()
            if (!className.isSpecial()) {
                addCamelNames(result, className.asString(), validator)
            }
        }
    }

    private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")

    public fun getCamelNames(name: String, validator: JetNameValidator, startLowerCase: Boolean): List<String> {
        val result = ArrayList<String>()
        addCamelNames(result, name, validator, startLowerCase)
        return result
    }

    private fun addCamelNames(result: ArrayList<String>, name: String, validator: JetNameValidator, startLowerCase: Boolean = true) {
        if (name === "") return
        var s = deleteNonLetterFromString(name)

        for (prefix in ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue

            val len = prefix.length()
            if (len < s.length() && Character.isUpperCase(s.charAt(len))) {
                s = s.substring(len)
                break
            }
        }

        var upperCaseLetterBefore = false
        for (i in 0..s.length() - 1) {
            val c = s.charAt(i)
            val upperCaseLetter = Character.isUpperCase(c)

            if (i == 0) {
                addName(result, if (startLowerCase) decapitalize(s) else s, validator)
            }
            else {
                if (upperCaseLetter && !upperCaseLetterBefore) {
                    val substring = s.substring(i)
                    addName(result, if (startLowerCase) decapitalize(substring) else substring, validator)
                }
            }

            upperCaseLetterBefore = upperCaseLetter
        }
    }

    private fun decapitalize(s: String): String {
        var c = s.charAt(0)
        if (!Character.isUpperCase(c)) return s

        val builder = StringBuilder(s.length())
        var decapitalize = true
        for (i in 0..s.length() - 1) {
            c = s.charAt(i)
            if (decapitalize) {
                if (Character.isUpperCase(c)) {
                    c = Character.toLowerCase(c)
                }
                else {
                    decapitalize = false
                }
            }
            builder.append(c)
        }
        return builder.toString()
    }

    private fun deleteNonLetterFromString(s: String): String {
        val pattern = Pattern.compile("[^a-zA-Z]")
        val matcher = pattern.matcher(s)
        return matcher.replaceAll("")
    }

    private fun addNamesForExpression(result: ArrayList<String>, expression: JetExpression?, validator: JetNameValidator) {
        if (expression == null) return

        expression.accept(object : JetVisitorVoid() {
            override fun visitQualifiedExpression(expression: JetQualifiedExpression) {
                val selectorExpression = expression.getSelectorExpression()
                addNamesForExpression(result, selectorExpression, validator)
            }

            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val referenceName = expression.getReferencedName()
                if (referenceName == referenceName.toUpperCase()) {
                    addName(result, referenceName, validator)
                }
                else {
                    addCamelNames(result, referenceName, validator)
                }
            }

            override fun visitCallExpression(expression: JetCallExpression) {
                addNamesForExpression(result, expression.getCalleeExpression(), validator)
            }

            override fun visitPostfixExpression(expression: JetPostfixExpression) {
                addNamesForExpression(result, expression.getBaseExpression(), validator)
            }
        })
    }

    public fun isIdentifier(name: String?): Boolean {
        ApplicationManager.getApplication().assertReadAccessAllowed()
        if (name == null || name.isEmpty()) return false

        val lexer = JetLexer()
        lexer.start(name, 0, name.length())
        if (lexer.getTokenType() !== JetTokens.IDENTIFIER) return false
        lexer.advance()
        return lexer.getTokenType() == null
    }
}
