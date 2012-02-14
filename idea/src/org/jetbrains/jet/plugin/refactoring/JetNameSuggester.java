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

package org.jetbrains.jet.plugin.refactoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Alefas
 * Date: 31.01.12
 */
public class JetNameSuggester {
    private JetNameSuggester() {
    }

    private static void addName(ArrayList<String> result, String name, JetNameValidator validator) {
        if (name == "class") name = "clazz";
        if (!isIdentifier(name)) return;
        String newName = validator.validateName(name);
        if (newName == null) return;
        result.add(newName);
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
     * @param validator to check scope for such names
     * @return possible names
     */
    public static String[] suggestNames(JetExpression expression, JetNameValidator validator) {
        ArrayList<String> result = new ArrayList<String>();

        BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache((JetFile) expression.getContainingFile(),
                                                                            AnalyzerFacade.SINGLE_DECLARATION_PROVIDER);
        JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
        if (jetType != null) {
            addNamesForType(result, jetType, validator);
        }
        addNamesForExpression(result, expression, validator);

        if (result.isEmpty()) addName(result, "value", validator);
        return ArrayUtil.toStringArray(result);
    }
    
    private static void addNamesForType(ArrayList<String> result, JetType jetType, JetNameValidator validator) {
        JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(validator.getProject());
        JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
        if (typeChecker.equalTypes(standardLibrary.getBooleanType(), jetType)) {
            addName(result, "b", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getIntType(), jetType)) {
            addName(result, "i", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getByteType(), jetType)) {
            addName(result, "byte", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getLongType(), jetType)) {
            addName(result, "l", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getFloatType(), jetType)) {
            addName(result, "fl", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getDoubleType(), jetType)) {
            addName(result, "d", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getShortType(), jetType)) {
            addName(result, "sh", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getCharType(), jetType)) {
            addName(result, "c", validator);
        } else if (typeChecker.equalTypes(standardLibrary.getStringType(), jetType)) {
            addName(result, "s", validator);
        } else {
            if (jetType.getArguments().size() == 1) {
                JetType argument = jetType.getArguments().get(0).getType();
                if (typeChecker.equalTypes(standardLibrary.getArrayType(argument), jetType)) {
                    if (typeChecker.equalTypes(standardLibrary.getBooleanType(), argument)) {
                        addName(result, "booleans", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getIntType(), argument)) {
                        addName(result, "ints", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getByteType(), argument)) {
                        addName(result, "bytes", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getLongType(), argument)) {
                        addName(result, "longs", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getFloatType(), argument)) {
                        addName(result, "floats", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getDoubleType(), argument)) {
                        addName(result, "doubles", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getShortType(), argument)) {
                        addName(result, "shorts", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getCharType(), argument)) {
                        addName(result, "chars", validator);
                    } else if (typeChecker.equalTypes(standardLibrary.getStringType(), argument)) {
                        addName(result, "strings", validator);
                    } else {
                        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(argument);
                        if (classDescriptor != null) {
                            String className = classDescriptor.getName();
                            addName(result, "arrayOf" + StringUtil.capitalize(className) + "s", validator);
                        }
                    }
                } else {
                    addForClassType(result, jetType, validator);
                }
            } else {
                addForClassType(result, jetType, validator);
            }
        }


    }

    private static void addForClassType(ArrayList<String> result, JetType jetType, JetNameValidator validator) {
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(jetType);
        if (classDescriptor != null) {
            String className = classDescriptor.getName();
            addCamelNames(result, className, validator);
        }
    }

    private static void addCamelNames(ArrayList<String> result, String name, JetNameValidator validator) {
        if (name == "") return;
        String s = deleteNonLetterFromString(name);
        if (s.startsWith("get") || s.startsWith("set")) s = s.substring(0, 3);
        else if (s.startsWith("is")) s = s.substring(0, 2);
        for (int i = 0; i < s.length(); ++i) {
            if (i == 0) {
                addName(result, StringUtil.decapitalize(s), validator);
            } else if (s.charAt(i) >= 'A' && s.charAt(i) <= 'Z') {
                addName(result, StringUtil.decapitalize(s.substring(i)), validator);
            }
        }
    }
    
    private static String deleteNonLetterFromString(String s) {
        Pattern pattern = Pattern.compile("[^a-zA-Z]");
        Matcher matcher = pattern.matcher(s);
        return matcher.replaceAll("");
    }
    
    private static void addNamesForExpression(ArrayList<String> result, JetExpression expression, JetNameValidator validator) {
        if (expression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) expression;
            addNamesForExpression(result, qualifiedExpression.getSelectorExpression(), validator);
        } else if (expression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression reference = (JetSimpleNameExpression) expression;
            String referenceName = reference.getReferencedName();
            if (referenceName == null) return;
            if (referenceName.equals(referenceName.toUpperCase())) {
                addName(result, referenceName, validator);
            } else {
                addCamelNames(result, referenceName, validator);
            }
        } else if (expression instanceof JetCallExpression) {
            JetCallExpression call = (JetCallExpression) expression;
            addNamesForExpression(result, call.getCalleeExpression(), validator);
        }
    }
    
    public static boolean isIdentifier(String name) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        if (name == null || name.isEmpty()) return false;

        JetLexer lexer = new JetLexer();
        lexer.start(name, 0, name.length());
        if (lexer.getTokenType() != JetTokens.IDENTIFIER) return false;
        lexer.advance();
        return lexer.getTokenType() == null;
    }
}
