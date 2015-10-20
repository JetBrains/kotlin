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

package org.jetbrains.kotlin.codegen.binding;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.fileClasses.FileClasses;
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;

public final class PsiCodegenPredictor {
    private PsiCodegenPredictor() {
    }

    public static boolean checkPredictedNameFromPsi(
            @NotNull DeclarationDescriptor descriptor,
            @Nullable Type nameFromDescriptors,
            @NotNull JvmFileClassesProvider fileClassesManager
    ) {
        PsiElement element = descriptorToDeclaration(descriptor);
        if (element instanceof KtDeclaration) {
            String classNameFromPsi = getPredefinedJvmInternalName((KtDeclaration) element, fileClassesManager);
            assert classNameFromPsi == null || Type.getObjectType(classNameFromPsi).equals(nameFromDescriptors) :
                    String.format("Invalid algorithm for getting qualified name from psi! Predicted: %s, actual %s\n" +
                                  "Element: %s", classNameFromPsi, nameFromDescriptors, element.getText());
        }

        return true;
    }

    /**
     * @return null if no prediction can be done.
     */
    @Nullable
    public static String getPredefinedJvmInternalName(
            @NotNull KtDeclaration declaration,
            @NotNull JvmFileClassesProvider fileClassesProvider
    ) {
        // TODO: Method won't work for declarations inside companion objects
        // TODO: Method won't give correct class name for traits implementations

        KtDeclaration parentDeclaration = KtStubbedPsiUtil.getContainingDeclaration(declaration);

        String parentInternalName;
        if (parentDeclaration != null) {
            parentInternalName = getPredefinedJvmInternalName(parentDeclaration, fileClassesProvider);
            if (parentInternalName == null) {
                return null;
            }
        }
        else {
            KtFile containingFile = declaration.getContainingJetFile();

            if (declaration instanceof KtNamedFunction) {
                Name name = ((KtNamedFunction) declaration).getNameAsName();
                return name == null ? null : FileClasses.getFileClassInternalName(fileClassesProvider, containingFile) + "$" + name.asString();
            }

            parentInternalName = AsmUtil.internalNameByFqNameWithoutInnerClasses(containingFile.getPackageFqName());
        }

        if (!PsiTreeUtil.instanceOf(declaration, KtClass.class, KtObjectDeclaration.class, KtNamedFunction.class, KtProperty.class) ||
            isEnumEntryWithoutBody(declaration)) {
            // Other subclasses are not valid for class name prediction.
            // For example JetFunctionLiteral
            return null;
        }

        Name name = ((KtNamedDeclaration) declaration).getNameAsName();
        if (name == null) {
            return null;
        }

        if (declaration instanceof KtNamedFunction) {
            if (!(parentDeclaration instanceof KtClass || parentDeclaration instanceof KtObjectDeclaration)) {
                // Can't generate predefined name for internal functions
                return null;
            }
        }

        // NOTE: looks like a bug - for class in getter of top level property class name will be $propertyName$ClassName but not
        // PackageClassName$propertyName$ClassName
        if (declaration instanceof KtProperty) {
            return parentInternalName + "$" + name.asString();
        }

        if (parentInternalName.isEmpty()) {
            return name.asString();
        }

        return parentInternalName + (parentDeclaration == null ? "/" : "$") + name.asString();
    }

    private static boolean isEnumEntryWithoutBody(KtDeclaration declaration) {
        if (!(declaration instanceof KtEnumEntry)) {
            return false;
        }
        KtClassBody body = ((KtEnumEntry) declaration).getBody();
        return body == null || body.getDeclarations().size() == 0;
    }
}
