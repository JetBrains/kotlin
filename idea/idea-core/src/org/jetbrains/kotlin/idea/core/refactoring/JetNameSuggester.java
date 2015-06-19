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

package org.jetbrains.kotlin.idea.core.refactoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.lexer.JetLexer;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JetNameSuggester {
    private JetNameSuggester() {
    }

    private static void addName(ArrayList<String> result, @Nullable String name, JetNameValidator validator) {
        if (name == null) return;
        if ("class".equals(name)) name = "clazz";
        if (!isIdentifier(name)) return;
        result.add(validator.validateName(name));
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
     * @param defaultName
     * @return possible names
     */
    public static @NotNull String[] suggestNames(@NotNull JetExpression expression, @NotNull JetNameValidator validator, @Nullable String defaultName) {
        ArrayList<String> result = new ArrayList<String>();

        BindingContext bindingContext = ResolvePackage.analyze(expression, BodyResolveMode.FULL);
        JetType jetType = bindingContext.getType(expression);
        if (jetType != null) {
            addNamesForType(result, jetType, validator);
        }
        addNamesForExpression(result, expression, validator);

        if (result.isEmpty()) addName(result, defaultName, validator);
        return ArrayUtil.toStringArray(result);
    }

    public static @NotNull String[] suggestNames(@NotNull JetType type, @NotNull JetNameValidator validator, @Nullable String defaultName) {
        ArrayList<String> result = new ArrayList<String>();
        addNamesForType(result, type, validator);
        if (result.isEmpty()) addName(result, defaultName, validator);
        return ArrayUtil.toStringArray(result);
    }

    public static @NotNull String[] suggestNamesForType(@NotNull JetType jetType, @NotNull JetNameValidator validator) {
        ArrayList<String> result = new ArrayList<String>();
        addNamesForType(result, jetType, validator);
        return ArrayUtil.toStringArray(result);
    }

    public static @NotNull String[] suggestNamesForExpression(@NotNull JetExpression expression, @NotNull JetNameValidator validator) {
        return suggestNamesForExpression(expression, validator, null);
    }

    public static @NotNull String[] suggestNamesForExpression(@NotNull JetExpression expression, @NotNull JetNameValidator validator, @Nullable String defaultName) {
        ArrayList<String> result = new ArrayList<String>();
        addNamesForExpression(result, expression, validator);
        if (result.isEmpty()) addName(result, defaultName, validator);
        return ArrayUtil.toStringArray(result);
    }

    private static final String[] COMMON_TYPE_PARAMETER_NAMES = {"T", "U", "V", "W", "X", "Y", "Z"};

    public static @NotNull String[] suggestNamesForTypeParameters(int count, @NotNull JetNameValidator validator) {
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            result.add(validator.validateNameWithVariants(COMMON_TYPE_PARAMETER_NAMES));
        }
        return ArrayUtil.toStringArray(result);
    }

    private static void addNamesForType(ArrayList<String> result, JetType jetType, JetNameValidator validator) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        JetTypeChecker typeChecker = JetTypeChecker.DEFAULT;
        jetType = TypeUtils.makeNotNullable(jetType); // wipe out '?'
        if (ErrorUtils.containsErrorType(jetType)) return;
        if (typeChecker.equalTypes(builtIns.getBooleanType(), jetType)) {
            addName(result, "b", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getIntType(), jetType)) {
            addName(result, "i", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getByteType(), jetType)) {
            addName(result, "byte", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getLongType(), jetType)) {
            addName(result, "l", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getFloatType(), jetType)) {
            addName(result, "fl", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getDoubleType(), jetType)) {
            addName(result, "d", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getShortType(), jetType)) {
            addName(result, "sh", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getCharType(), jetType)) {
            addName(result, "c", validator);
        }
        else if (typeChecker.equalTypes(builtIns.getStringType(), jetType)) {
            addName(result, "s", validator);
        }
        else if (KotlinBuiltIns.isArray(jetType) || KotlinBuiltIns.isPrimitiveArray(jetType)) {
            JetType elementType = KotlinBuiltIns.getInstance().getArrayElementType(jetType);
            if (typeChecker.equalTypes(builtIns.getBooleanType(), elementType)) {
                addName(result, "booleans", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getIntType(), elementType)) {
                addName(result, "ints", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getByteType(), elementType)) {
                addName(result, "bytes", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getLongType(), elementType)) {
                addName(result, "longs", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getFloatType(), elementType)) {
                addName(result, "floats", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getDoubleType(), elementType)) {
                addName(result, "doubles", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getShortType(), elementType)) {
                addName(result, "shorts", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getCharType(), elementType)) {
                addName(result, "chars", validator);
            }
            else if (typeChecker.equalTypes(builtIns.getStringType(), elementType)) {
                addName(result, "strings", validator);
            }
            else {
                ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(elementType);
                if (classDescriptor != null) {
                    Name className = classDescriptor.getName();
                    addName(result, "arrayOf" + StringUtil.capitalize(className.asString()) + "s", validator);
                }
            }
        }
        else {
            addForClassType(result, jetType, validator);
        }
    }

    private static void addForClassType(ArrayList<String> result, JetType jetType, JetNameValidator validator) {
        ClassifierDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (descriptor != null) {
            Name className = descriptor.getName();
            if (!className.isSpecial()) {
                addCamelNames(result, className.asString(), validator);
            }
        }
    }

    private static final String[] ACCESSOR_PREFIXES = { "get", "is", "set" };

    public static List<String> getCamelNames(String name, JetNameValidator validator, boolean startLowerCase) {
        ArrayList<String> result = new ArrayList<String>();
        addCamelNames(result, name, validator, startLowerCase);
        return result;
    }

    private static void addCamelNames(ArrayList<String> result, String name, JetNameValidator validator) {
        addCamelNames(result, name, validator, true);
    }

    private static void addCamelNames(ArrayList<String> result, String name, JetNameValidator validator, boolean startLowerCase) {
        if (name == "") return;
        String s = deleteNonLetterFromString(name);

        for (String prefix : ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue;

            int len = prefix.length();
            if (len < s.length() && Character.isUpperCase(s.charAt(len))) {
                s = s.substring(len);
                break;
            }
        }

        boolean upperCaseLetterBefore = false;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            boolean upperCaseLetter = Character.isUpperCase(c);

            if (i == 0) {
                addName(result, startLowerCase ? decapitalize(s) : s, validator);
            }
            else {
                if (upperCaseLetter && !upperCaseLetterBefore) {
                    String substring = s.substring(i);
                    addName(result, startLowerCase ? decapitalize(substring) : substring, validator);
                }
            }

            upperCaseLetterBefore = upperCaseLetter;
        }
    }

    public static String decapitalize(String s) {
        char c = s.charAt(0);
        if (!Character.isUpperCase(c)) return s;

        StringBuilder builder = new StringBuilder(s.length());
        boolean decapitalize = true;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (decapitalize) {
                if (Character.isUpperCase(c)) {
                    c = Character.toLowerCase(c);
                }
                else {
                    decapitalize = false;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }
    
    private static String deleteNonLetterFromString(String s) {
        Pattern pattern = Pattern.compile("[^a-zA-Z]");
        Matcher matcher = pattern.matcher(s);
        return matcher.replaceAll("");
    }
    
    private static void addNamesForExpression(
            final ArrayList<String> result,
            @Nullable JetExpression expression,
            final JetNameValidator validator) {
        if (expression == null) return;

        expression.accept(new JetVisitorVoid() {
            @Override
            public void visitQualifiedExpression(@NotNull JetQualifiedExpression expression) {
                JetExpression selectorExpression = expression.getSelectorExpression();
                addNamesForExpression(result, selectorExpression, validator);
            }

            @Override
            public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
                String referenceName = expression.getReferencedName();
                if (referenceName.equals(referenceName.toUpperCase())) {
                    addName(result, referenceName, validator);
                }
                else {
                    addCamelNames(result, referenceName, validator);
                }
            }

            @Override
            public void visitCallExpression(@NotNull JetCallExpression expression) {
                addNamesForExpression(result, expression.getCalleeExpression(), validator);
            }

            @Override
            public void visitPostfixExpression(@NotNull JetPostfixExpression expression) {
                addNamesForExpression(result, expression.getBaseExpression(), validator);
            }
        });
    }
    
    public static boolean isIdentifier(@Nullable String name) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        if (name == null || name.isEmpty()) return false;

        JetLexer lexer = new JetLexer();
        lexer.start(name, 0, name.length());
        if (lexer.getTokenType() != JetTokens.IDENTIFIER) return false;
        lexer.advance();
        return lexer.getTokenType() == null;
    }
}
