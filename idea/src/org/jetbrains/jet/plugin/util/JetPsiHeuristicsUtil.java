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

package org.jetbrains.jet.plugin.util;

import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.jet.asJava.KotlinLightClassForPackage;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.QualifiedNamesUtil;

public class JetPsiHeuristicsUtil {
    private JetPsiHeuristicsUtil() {}

    public static boolean isAccessible(@NotNull PsiMember member, @NotNull JetFile fromFile) {
        if (member instanceof KotlinLightClassForPackage) {
            // Package classes are only available from Java, so they should not be visible from Kotlin
            return false;
        }
        if (member instanceof KotlinLightClassForExplicitDeclaration) {
            KotlinLightClassForExplicitDeclaration lightClass = (KotlinLightClassForExplicitDeclaration) member;

            // It is a Kotlin class already, we need to properly check visibility?
            JetClassOrObject classOrObject = lightClass.getJetClassOrObject();

            if (isTopLevelDeclaration(classOrObject) && classOrObject.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
                // The class is declared private in the targetPackage
                // It is visible in this package and all of its subpackages
                JetFile targetFile = (JetFile) classOrObject.getContainingFile();
                FqName targetPackage = JetPsiUtil.getFQName(targetFile);
                FqName fromPackage = JetPsiUtil.getFQName(fromFile);
                return QualifiedNamesUtil.isSubpackageOf(fromPackage, targetPackage);
            }
        }
        return member.hasModifierProperty(PsiModifier.PUBLIC) || member.hasModifierProperty(PsiModifier.PROTECTED);
    }

    public static boolean isTopLevelDeclaration(@NotNull JetClassOrObject classOrObject) {
        return classOrObject.getContainingFile() == classOrObject.getParent();
    }
}
